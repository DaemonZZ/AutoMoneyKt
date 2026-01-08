package com.daemonz.strategies.ema_pullback_v7

import com.daemonz.core.analysis.CompatibilityScore
import com.daemonz.core.analysis.MarketStats
import com.daemonz.core.analysis.StrategyCompatibility

class EmaPullbackV7Compatibility : StrategyCompatibility<EmaPullbackV7Params> {

    override fun compatibility(stats: MarketStats, params: EmaPullbackV7Params): CompatibilityScore {
        val reasons = mutableListOf<String>()

        // --- gates (ghi reasons, nhưng không "đóng băng" score dưới 30 nữa) ---
        if (stats.atrPct < params.minAtrPct) reasons += "ATR too low"
        if (stats.atrPct > params.maxAtrPct) reasons += "ATR too high"
        if (stats.chopScore > params.maxChopScore) reasons += "Choppy"
        if (stats.trendStrength < params.minTrendStrength) reasons += "Weak trend"

        // 1) Baseline cho market "bình thường"
        var score = 45

        // 2) Trend là yếu tố chính
        score += (stats.trendStrength * 40).toInt()   // 0..+40

        // 3) Chop penalty theo tier (trừ mạnh khi quá chop)
        score -= when {
            stats.chopScore > 0.75 -> 30
            stats.chopScore > 0.65 -> 20
            stats.chopScore > 0.55 -> 10
            else -> 0
        }

        // 4) ATR hỗ trợ nhẹ
        val atrScore = atrSweetSpotScore(stats.atrPct, params.minAtrPct, params.maxAtrPct)
        score += (atrScore * 0.20).toInt()            // 0..+20

        // 5) Hard disqualifier nhẹ để tạo phân tầng (đừng dìm tất cả về < 30)
        if (stats.trendStrength < params.minTrendStrength * 0.8) score -= 20
        if (stats.atrPct < params.minAtrPct * 0.7) score -= 15
        if (stats.atrPct > params.maxAtrPct * 1.2) score -= 15

        score = score.coerceIn(0, 100)

        val shortReasons = buildList {
            add("Compat=$score")
            addAll(reasons.distinct().take(2).ifEmpty { listOf("OK") })
            add(
                "ATR%=${"%.2f".format(stats.atrPct)} " +
                        "Chop=${"%.2f".format(stats.chopScore)} " +
                        "Trend=${"%.2f".format(stats.trendStrength)}"
            )
        }.take(4)

        return CompatibilityScore(
            score = score,
            reasons = shortReasons,
            needBacktest = score >= 70
        )
    }


    private fun atrSweetSpotScore(atrPct: Double, minAtr: Double, maxAtr: Double): Int {
        // Điểm cao nhất ở giữa khoảng [minAtr..maxAtr]
        if (maxAtr <= minAtr) return 50
        val mid = (minAtr + maxAtr) / 2.0
        val half = (maxAtr - minAtr) / 2.0
        val d = kotlin.math.abs(atrPct - mid) / half
        val s = ((1.0 - d.coerceIn(0.0, 1.0)) * 100.0).toInt()
        return s.coerceIn(0, 100)
    }
}
