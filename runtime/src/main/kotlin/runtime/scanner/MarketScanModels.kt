package com.daemonz.runtime.scanner

import com.daemonz.core.market.Timeframe

enum class Verdict { TRADE, WATCH, SKIP }

data class ScanRequest(
    val symbols: List<String>? = null,     // null => auto pick
    val autoPickCount: Int = 5,
    val interval: Timeframe = Timeframe.M15,
    val candleLimit: Int = 1500
)

data class ScanMetrics(
    val trades: Int,
    val winRate: Double,
    val profitFactor: Double,
    val expectancy: Double,     // avg pnl per trade
    val maxDrawdownPct: Double,
    val netPnl: Double
) {
    companion object {
        /**
         * Step A (market-only) has no backtest metrics.
         * Use zeros so UI keeps working without change.
         */
        val EMPTY = ScanMetrics(
            trades = 0,
            winRate = 0.0,
            profitFactor = 0.0,
            expectancy = 0.0,
            maxDrawdownPct = 0.0,
            netPnl = 0.0
        )
    }
}


data class ScanResult(
    val symbol: String,
    val metrics: ScanMetrics,     // Step A dùng ScanMetrics.EMPTY
    val score: Int,               // 0..100 (Step A = confidence * 100)
    val verdict: Verdict,         // TRADE / SKIP
    val reasons: List<String>     // 2–4 dòng ngắn
)