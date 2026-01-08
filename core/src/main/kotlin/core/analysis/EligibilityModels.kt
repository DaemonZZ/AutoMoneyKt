package com.daemonz.core.analysis

data class EligibilityResult(
    val eligible: Boolean,
    val regime: Regime,
    val confidence: Double,      // 0..1
    val reasons: List<String>,
    val stats: MarketStats
)