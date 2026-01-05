package com.daemonz.runtime.scanner

import com.daemonz.core.market.Timeframe

enum class Verdict { TRADE, WATCH, AVOID }

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
)

data class ScanResult(
    val symbol: String,
    val metrics: ScanMetrics,
    val score: Int,             // 0..100
    val verdict: Verdict,
    val reasons: List<String>   // 2-4 dòng ngắn
)