package com.daemonz.core.analysis

import kotlin.math.abs

class EligibilityEngine(
    private val th: EligibilityThresholds = EligibilityThresholds()
) {
    fun evaluate(stats: MarketStats): EligibilityResult {
        val reasons = mutableListOf<String>()

        if (stats.atrPct < th.minAtrPct) reasons += "ATR% too low (${fmt(stats.atrPct)})"
        if (stats.atrPct > th.maxAtrPct) reasons += "ATR% too high (${fmt(stats.atrPct)})"
        if (stats.chopScore > th.maxChopScore) reasons += "Too choppy (${fmt(stats.chopScore)})"

        val regime = when {
            stats.trendStrength >= th.minTrendStrength && stats.chopScore <= th.trendChopUpper -> Regime.TREND
            stats.chopScore >= th.chopHard -> Regime.CHOP
            else -> Regime.RANGE
        }

        val eligible = reasons.isEmpty()

        // Confidence (0..1): blend of trendStrength, (1-chop), and ATR closeness to mid range.
        val atrMid = (th.minAtrPct + th.maxAtrPct) / 2.0
        val atrOk = (1.0 - (abs(stats.atrPct - atrMid) / atrMid.coerceAtLeast(1e-9)))
            .coerceIn(0.0, 1.0)

        val confidence = (
                0.45 * stats.trendStrength +
                        0.35 * (1.0 - stats.chopScore) +
                        0.20 * atrOk
                ).coerceIn(0.0, 1.0)

        return EligibilityResult(
            eligible = eligible,
            regime = regime,
            confidence = confidence,
            reasons = reasons,
            stats = stats
        )
    }

    private fun fmt(x: Double) = "%.2f".format(x)
}