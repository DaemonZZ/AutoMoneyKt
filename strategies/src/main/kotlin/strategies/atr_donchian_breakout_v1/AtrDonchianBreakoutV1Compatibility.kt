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


        val hardFail = (stats.atrPct < params.minAtrPct) || (stats.atrPct > params.maxAtrPct)

        val needBacktest = !hardFail && finalScore >= 40
        val failAtrLow = stats.atrPct < params.minAtrPct
        val failAtrHigh = stats.atrPct > params.maxAtrPct
        val failTrend = stats.trendStrength < params.minTrendStrength
        val failChop = stats.chopScore > params.maxChopScore
        val failLiq = stats.liquidityScore < params.minLiquidityScore

        println(
            """
    ─────────────────────────────────────────
    [ATR DONCHIAN COMPAT]
    Symbol      : previous log
    ATR%        : ${stats.atrPct}   (min=${params.minAtrPct}, max=${params.maxAtrPct})
    Trend       : ${stats.trendStrength} (min=${params.minTrendStrength})
    Chop        : ${stats.chopScore} (max=${params.maxChopScore})
    Liquidity   : ${stats.liquidityScore} (min=${params.minLiquidityScore})

    Fail Gates:
      ATR low   : $failAtrLow
      ATR high  : $failAtrHigh
      Trend     : $failTrend
      Chop      : $failChop
      Liquidity : $failLiq

    Score Calc:
      Base      : 100
      ATR pen   : ${
                (if (failAtrLow) -35 else 0) + (if (failAtrHigh) -30 else 0)
            }
      Chop pen  : ${-(stats.chopScore * 25)}
      Trend bon : ${stats.trendStrength * 20}
      Liq bon   : ${stats.liquidityScore * 10}
      ATR sweet : ${
                if (stats.atrPct in (params.minAtrPct + 0.2)..(params.maxAtrPct - 0.5)) 15 else 0
            }

    FinalScore  : $finalScore
    HardFail    : $hardFail
    NeedBacktest: $needBacktest
    Reasons     : $reasons
    ─────────────────────────────────────────
    """.trimIndent()
        )

        return CompatibilityScore(
            score = finalScore,
            reasons = reasons.take(3).ifEmpty { listOf("OK") },
            needBacktest = needBacktest
        )
    }
}
