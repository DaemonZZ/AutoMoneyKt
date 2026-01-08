package com.daemonz.core.analysis

data class TuningSummary(
    val n: Int,
    val atrP10: Double, val atrP50: Double, val atrP90: Double,
    val chopP10: Double, val chopP50: Double, val chopP90: Double,
    val trendP10: Double, val trendP50: Double, val trendP90: Double
)

class StatsCollector(
    private val maxItems: Int = 5000
) {
    private val atr = ArrayList<Double>(maxItems)
    private val chop = ArrayList<Double>(maxItems)
    private val trend = ArrayList<Double>(maxItems)

    @Synchronized
    fun add(stats: MarketStats) {
        if (atr.size >= maxItems) return
        atr += stats.atrPct
        chop += stats.chopScore
        trend += stats.trendStrength
    }

    @Synchronized
    fun summary(): TuningSummary? {
        if (atr.isEmpty()) return null
        val a = atr.sorted()
        val c = chop.sorted()
        val t = trend.sorted()
        return TuningSummary(
            n = a.size,
            atrP10 = q(a, 0.10), atrP50 = q(a, 0.50), atrP90 = q(a, 0.90),
            chopP10 = q(c, 0.10), chopP50 = q(c, 0.50), chopP90 = q(c, 0.90),
            trendP10 = q(t, 0.10), trendP50 = q(t, 0.50), trendP90 = q(t, 0.90)
        )
    }

    private fun q(sorted: List<Double>, p: Double): Double {
        if (sorted.isEmpty()) return 0.0
        val idx = ((sorted.size - 1) * p).toInt().coerceIn(0, sorted.size - 1)
        return sorted[idx]
    }
}
