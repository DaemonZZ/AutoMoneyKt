package com.daemonz.runtime.scaner

import com.daemonz.adapters.exchange.ExchangeAdapter
import com.daemonz.core.engine.BacktestEngine
import com.daemonz.core.risk.RiskConfig
import com.daemonz.core.strategy.Strategy
import kotlinx.coroutines.*
import kotlin.random.Random

class MarketScannerService(
    private val exchange: ExchangeAdapter,
    private val risk: RiskConfig = RiskConfig()
) {
    suspend fun <P> analyze(
        req: ScanRequest,
        strategy: Strategy<P>,
        params: P
    ): List<ScanResult> = coroutineScope {
        val symbols = req.symbols ?: autoPick(req.autoPickCount)

        symbols.map { symbol ->
            async(Dispatchers.IO) {
                val candles = exchange.fetchCandles(symbol, req.interval, req.candleLimit)
                val engine = BacktestEngine(risk)
                val result = engine.run(symbol, candles, strategy, params)

                val metrics = Stats.compute(
                    trades = result.trades,
                    startingEquity = risk.startingEquity,
                    endingEquity = result.endingEquity
                )
                val (score, verdictAndReasons) = Scoring.score(metrics)
                val (verdict, reasons) = verdictAndReasons

                ScanResult(symbol, metrics, score, verdict, reasons)
            }
        }.awaitAll()
            .sortedWith(compareByDescending<ScanResult> { it.score }.thenByDescending { it.metrics.trades })
    }

    private fun autoPick(n: Int): List<String> {
        val all = exchange.listTradableSymbols()
        if (all.size <= n) return all
        // random pick basic; sau này lọc theo volatility/liquidity
        return all.shuffled(Random(System.currentTimeMillis())).take(n)
    }
}