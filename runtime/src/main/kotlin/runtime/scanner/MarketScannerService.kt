package com.daemonz.runtime.scanner

import com.daemonz.adapters.exchange.ExchangeAdapter
import com.daemonz.core.engine.BacktestEngine
import com.daemonz.core.engine.BacktestResult
import com.daemonz.core.market.Timeframe
import com.daemonz.core.risk.RiskConfig
import com.daemonz.core.strategy.Strategy
import com.daemonz.core.trade.Trade
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.sync.withPermit

class MarketScannerService(
    private val exchange: ExchangeAdapter,
    private val risk: RiskConfig = RiskConfig()
) {
    suspend fun <P> analyze(
        req: ScanRequest,
        strategy: Strategy<P>,
        params: P,
        onProgress: suspend (done: Int, total: Int, symbol: String) -> Unit = { _, _, _ -> },
        onDetail: suspend (symbol: String, backtest: BacktestResult) -> Unit = { _, _ -> },
        onVolatility: suspend (symbol: String, volPct: Double) -> Unit = { _, _ -> }   // ✅ NEW
    ): List<ScanResult> = coroutineScope {

        val symbols = req.symbols ?: autoPick(req.autoPickCount)
        val total = symbols.size
        var done = 0

        symbols.map { symbol ->
            async(Dispatchers.IO) {
                onProgress(done, total, symbol)

                val candles = exchange.fetchCandles(
                    symbol,
                    req.interval,
                    req.candleLimit
                )
                val volPct = calcVolatilityPct(candles, lookback = 50)
                onVolatility(symbol, volPct)

                val engine = BacktestEngine(risk)
                val bt = engine.run(symbol, candles, strategy, params)
                onDetail(symbol, bt)

                val metrics = computeMetrics(
                    bt.trades,
                    risk.startingEquity,
                    bt.endingEquity
                )

                val (score, verdict, reasons) = score(metrics)

                synchronized(this@MarketScannerService) {
                    done += 1
                }

                onProgress(done, total, symbol)

                ScanResult(
                    symbol = symbol,
                    metrics = metrics,
                    score = score,
                    verdict = verdict,
                    reasons = reasons
                )
            }
        }.awaitAll()
    }

    /**
     * AUTO-PICK mode:
     * - Tự chọn pool ứng viên theo logic + exclusions
     * - Backtest pool và lọc ra đúng `auto.count` symbol có verdict=TRADE
     * - Nếu chưa đủ TRADE thì mở rộng pool dần tới maxPoolSize
     */
    suspend fun <P> autoPickAnalyzeTopTrades(
        req: ScanRequest,
        auto: AutoPickConfig,
        strategy: Strategy<P>,
        params: P,
        onProgress: suspend (done: Int, total: Int, symbol: String) -> Unit = { _, _, _ -> },
        onDetail: suspend (symbol: String, backtest: BacktestResult) -> Unit = { _, _ -> },
        onVolatility: suspend (symbol: String, volPct: Double) -> Unit = { _, _ -> }  // ✅ NEW
    ): List<ScanResult> = coroutineScope {

        val candidatesAll = autoPickCandidates(auto, req.interval)
        if (candidatesAll.isEmpty()) return@coroutineScope emptyList()

        val engine = BacktestEngine(risk)
        val sem = Semaphore(permits = 6)

        val analyzed = LinkedHashMap<String, ScanResult>()  // giữ scan theo symbol
        val tradeHits = ArrayList<ScanResult>()

        var pool = auto.poolSize.coerceAtLeast(auto.count).coerceAtMost(auto.maxPoolSize)
        var cursor = 0

        while (tradeHits.size < auto.count && cursor < candidatesAll.size && pool <= auto.maxPoolSize) {
            val end = (cursor + pool).coerceAtMost(candidatesAll.size)
            val batch = candidatesAll.subList(cursor, end)

            val total = end // progress theo số đã “đụng tới” trong list ứng viên
            var done = cursor

            val batchResults = batch.map { symbol ->
                async(Dispatchers.IO) {
                    sem.withPermit {
                        val candles = exchange.fetchCandles(
                            symbol = symbol,
                            interval = req.interval,
                            limit = req.candleLimit.coerceAtMost(1500)
                        )
                        val volPct = calcVolatilityPct(candles, lookback = 50)
                        onVolatility(symbol, volPct)

                        val bt = engine.run(symbol, candles, strategy, params)
                        onDetail(symbol, bt)

                        val metrics = computeMetrics(
                            trades = bt.trades,
                            startingEquity = risk.startingEquity,
                            endingEquity = bt.endingEquity
                        )
                        val (score, verdict, reasons) = score(metrics)

                        val scan = ScanResult(
                            symbol = symbol,
                            metrics = metrics,
                            score = score,
                            verdict = verdict,
                            reasons = reasons
                        )

                        synchronized(this@MarketScannerService) {
                            analyzed[symbol] = scan
                            done += 1
                        }
                        onProgress(done, total, symbol)

                        scan
                    }
                }
            }.awaitAll()

            // lấy TRADE từ batch
            batchResults
                .filter { it.verdict == Verdict.TRADE }
                .forEach { tradeHits.add(it) }

            // nếu đủ rồi thì break
            if (tradeHits.size >= auto.count) break

            // chưa đủ => mở rộng: tăng cursor và pool step
            cursor = end
            pool = (pool + auto.stepSize).coerceAtMost(auto.maxPoolSize)
        }

        // Nếu có nhiều hơn count, sort lấy top
        val picked = tradeHits
            .distinctBy { it.symbol }
            .sortedWith(
                compareByDescending<ScanResult> { it.score }
                    .thenByDescending { it.metrics.profitFactor }
                    .thenBy { it.metrics.maxDrawdownPct }
            )
            .take(auto.count)

        picked
    }

    /**
     * Build candidates list theo AutoPickLogic + exclusions.
     * Ưu tiên ticker24h để có TOP_VOLUME / GAINERS.
     */
    private fun autoPickCandidates(
        auto: AutoPickConfig,
        interval: Timeframe
    ): List<String> {
        val tradable = exchange.listTradableSymbols().toSet()
        val tickers = exchange.fetchTickers24h()
            .asSequence()
            .map { it }
            .filter { tradable.contains(it.symbol) }
            .filter { it.symbol.endsWith("USDT") }  // futures USDT-M
            .filter { passesExclusions(it.symbol, auto) }
            .toList()

        if (tickers.isEmpty()) return emptyList()

        return when (auto.logic) {
            AutoPickLogic.TOP_BY_VOLUME ->
                tickers.sortedByDescending { it.quoteVolume }.map { it.symbol }

            AutoPickLogic.TOP_GAINERS_24H ->
                tickers.sortedByDescending { it.priceChangePercent }.map { it.symbol }

            AutoPickLogic.RANDOM_DIVERSITY -> {
                val list = tickers.map { it.symbol }.distinct()
                list.shuffled(Random(System.currentTimeMillis()))
            }
        }
    }

    private fun passesExclusions(symbol: String, auto: AutoPickConfig): Boolean {
        if (auto.excludeStablecoins) {
            val base = symbol.removeSuffix("USDT")
            val stableBases = setOf("USDC", "BUSD", "FDUSD", "TUSD", "DAI", "USDP", "PAXG") // tuỳ bạn thêm/bớt
            if (stableBases.contains(base)) return false
        }

        if (auto.excludeLeveragedTokens) {
            val s = symbol.uppercase()
            val leveragedPatterns = listOf(
                "UPUSDT", "DOWNUSDT", "BULLUSDT", "BEARUSDT",
                "3LUSDT", "3SUSDT", "5LUSDT", "5SUSDT"
            )
            if (leveragedPatterns.any { s.endsWith(it) }) return false
        }

        return true
    }


    private fun autoPick(n: Int): List<String> {
        val all = exchange.listTradableSymbols()
        if (all.size <= n) return all
        // random pick basic; sau này lọc theo volatility/liquidity
        return all.shuffled(Random(System.currentTimeMillis())).take(n)
    }

    private fun computeMetrics(
        trades: List<Trade>,
        startingEquity: Double,
        endingEquity: Double
    ): ScanMetrics {
        val n = trades.size
        val net = endingEquity - startingEquity

        if (n == 0) {
            return ScanMetrics(
                trades = 0,
                winRate = 0.0,
                profitFactor = 0.0,
                expectancy = 0.0,
                maxDrawdownPct = 0.0,
                netPnl = net
            )
        }

        val wins = trades.count { it.pnl > 0 }
        val winRate = wins.toDouble() / n.toDouble()

        val grossProfit = trades.filter { it.pnl > 0 }.sumOf { it.pnl }
        val grossLoss = trades.filter { it.pnl < 0 }.sumOf { abs(it.pnl) }
        val profitFactor = if (grossLoss == 0.0) 99.0 else grossProfit / grossLoss

        val expectancy = trades.sumOf { it.pnl } / n.toDouble()

        // trade-by-trade equity drawdown (basic)
        var peak = startingEquity
        var equity = startingEquity
        var maxDd = 0.0
        for (t in trades) {
            equity += t.pnl
            peak = max(peak, equity)
            val dd = if (peak == 0.0) 0.0 else (peak - equity) / peak
            maxDd = max(maxDd, dd)
        }

        return ScanMetrics(
            trades = n,
            winRate = winRate,
            profitFactor = profitFactor,
            expectancy = expectancy,
            maxDrawdownPct = maxDd * 100.0,
            netPnl = net
        )
    }

    private fun score(m: ScanMetrics): Triple<Int, Verdict, List<String>> {
        val reasons = mutableListOf<String>()
        var s = 50.0

        if (m.trades < 10) {
            s -= 20
            reasons += "Low sample (${m.trades} trades)"
        } else if (m.trades > 40) {
            s += 5
            reasons += "Sample OK (${m.trades} trades)"
        }

        if (m.expectancy > 0) {
            s += 15
            reasons += "Expectancy +${fmt(m.expectancy)}"
        } else {
            s -= 15
            reasons += "Expectancy ${fmt(m.expectancy)}"
        }

        if (m.profitFactor >= 1.5) {
            s += 15
            reasons += "PF ${fmt(m.profitFactor)}"
        } else if (m.profitFactor < 1.0) {
            s -= 20
            reasons += "PF < 1 (${fmt(m.profitFactor)})"
        } else {
            s -= 5
            reasons += "PF ${fmt(m.profitFactor)}"
        }

        if (m.maxDrawdownPct <= 10.0) {
            s += 10
            reasons += "DD low (${fmt(m.maxDrawdownPct)}%)"
        } else if (m.maxDrawdownPct > 25.0) {
            s -= 25
            reasons += "DD high (${fmt(m.maxDrawdownPct)}%)"
        } else {
            s -= 5
            reasons += "DD ${fmt(m.maxDrawdownPct)}%"
        }

        // small effect for net pnl
        if (m.netPnl > 0) reasons += "Net +${fmt(m.netPnl)}" else reasons += "Net ${fmt(m.netPnl)}"

        val score = min(100, max(0, s.toInt()))
        val verdict = when {
            score >= 70 && m.trades >= 10 -> Verdict.TRADE
            score >= 50 -> Verdict.WATCH
            else -> Verdict.AVOID
        }

        return Triple(score, verdict, reasons.take(4))
    }

    private fun fmt(x: Double) = String.format("%.4f", x)
    private fun calcVolatilityPct(candles: List<com.daemonz.core.market.Candle>, lookback: Int = 50): Double {
        if (candles.size < 2) return 0.0
        val start = (candles.size - lookback).coerceAtLeast(1)

        var sum = 0.0
        var count = 0

        for (i in start until candles.size) {
            val cur = candles[i]
            val prev = candles[i - 1]

            val tr = maxOf(
                cur.high - cur.low,
                abs(cur.high - prev.close),
                abs(cur.low - prev.close)
            )

            val base = cur.close
            if (base > 0.0) {
                sum += (tr / base) * 100.0
                count++
            }
        }
        return if (count > 0) sum / count else 0.0
    }

}