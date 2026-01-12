package com.daemonz.strategies.atr_donchian_breakout_v1

import com.daemonz.core.analysis.CompatibilityScore
import com.daemonz.core.analysis.MarketStats
import com.daemonz.core.analysis.StrategyCompatibility
import kotlin.math.roundToInt

class AtrDonchianBreakoutV1Compatibility :
    StrategyCompatibility<AtrDonchianBreakoutV1CompatibilityParams> {

    override fun compatibility(
        stats: MarketStats,
        params: AtrDonchianBreakoutV1CompatibilityParams
    ): CompatibilityScore {

        val reasons = mutableListOf<String>()

        // 1) Hard gates (giống style EMA Pullback)
        if (stats.atrPct < params.minAtrPct) reasons += "ATR too low"
        if (stats.atrPct > params.maxAtrPct) reasons += "ATR too high"
        if (stats.trendStrength < params.minTrendStrength) reasons += "Weak trend"
        if (stats.chopScore > params.maxChopScore) reasons += "Choppy"
        if (stats.liquidityScore < params.minLiquidityScore) reasons += "Low liquidity"

        // 2) Score model (0..100)
        var score = 100.0

        // ATR outside band: phạt nặng
        if (stats.atrPct < params.minAtrPct) score -= 35.0
        if (stats.atrPct > params.maxAtrPct) score -= 30.0

        // Chop: kẻ thù số 1 của breakout
        score -= stats.chopScore * 25.0

        // Trend: bạn của breakout
        score += stats.trendStrength * 20.0

        // Liquidity: bonus nhẹ
        score += stats.liquidityScore * 10.0

        if (stats.atrPct in (params.minAtrPct + 0.2)..(params.maxAtrPct - 0.5)) {
            score += 15.0
        }

        val finalScore = score.coerceIn(0.0, 100.0).roundToInt()

        // 3) needBacktest heuristic
        // - nếu fail các gate quan trọng -> khỏi backtest (thường fake breakout/chop)
        // - nếu không fail hard, score trung bình -> nên backtest
        val hardFail =
            (stats.atrPct < params.minAtrPct) ||
                    (stats.atrPct > params.maxAtrPct) ||
                    (stats.chopScore > params.maxChopScore) ||
                    (stats.trendStrength < params.minTrendStrength)

        val needBacktest = !hardFail && finalScore in 40..90

        return CompatibilityScore(
            score = finalScore,
            reasons = reasons.take(3).ifEmpty { listOf("OK") },
            needBacktest = needBacktest
        )
    }
}
