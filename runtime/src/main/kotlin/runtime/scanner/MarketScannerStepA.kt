package com.daemonz.runtime.scanner

import com.daemonz.adapters.exchange.ExchangeAdapter
import com.daemonz.core.analysis.*
import com.daemonz.core.market.Timeframe
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

data class StepAConfig(
    val maxConcurrency: Int = 6,
    val applyFilters: Boolean = true
)

class MarketScannerStepA(
    private val exchange: ExchangeAdapter,
    private val statsComputer: MarketStatsComputer = SimpleMarketStatsComputer(),
    private val eligibilityEngine: EligibilityEngine = EligibilityEngine(),
    private val cache: StatsCache = LruStatsCache(maxSize = 3000),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun analyze(
        symbols: List<String>,
        timeframe: Timeframe,
        windowBars: Int,
        config: StepAConfig = StepAConfig(),
        onProgress: suspend (done: Int, total: Int, symbol: String) -> Unit = { _, _, _ -> },
        onResult: suspend (symbol: String, result: EligibilityResult) -> Unit = { _, _ -> },
        onError: suspend (symbol: String, error: Throwable) -> Unit = { _, _ -> }
    ) = coroutineScope {

        val total = symbols.size
        val doneCounter = AtomicInteger(0)
        val sem = Semaphore(config.maxConcurrency)

        symbols.map { symbol ->
            async(ioDispatcher) {
                sem.withPermit {
                    try {
                        // Use blocking adapter call on IO dispatcher
                        val candles = exchange.fetchCandles(symbol, timeframe, windowBars)

                        // Ensure ascending order (Binance already returns ascending, but keep safe)
                        val sorted = candles.sortedBy { it.t }
                        val lastCandleTime = sorted.last().t
                        val key = StatsKey(symbol, timeframe.name, windowBars, lastCandleTime)
                        val stats = cache.get(key) ?: statsComputer.compute(sorted).also { cache.put(key, it) }

                        var res = eligibilityEngine.evaluate(stats)

                        // If user disables filter: do not hard reject; still provide stats/reasons
                        if (!config.applyFilters) {
                            res = res.copy(eligible = true)
                        }

                        val done = doneCounter.incrementAndGet()
                        onProgress(done, total, symbol)
                        onResult(symbol, res)
                    } catch (t: Throwable) {
                        val done = doneCounter.incrementAndGet()
                        onProgress(done, total, symbol)
                        onError(symbol, t)
                    }
                }
            }
        }.awaitAll()
    }
}
