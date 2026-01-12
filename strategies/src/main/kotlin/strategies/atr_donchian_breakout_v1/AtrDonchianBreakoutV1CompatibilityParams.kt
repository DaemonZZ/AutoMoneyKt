package com.daemonz.strategies.atr_donchian_breakout_v1

data class AtrDonchianBreakoutV1CompatibilityParams(
    val minAtrPct: Double = 0.4,
    val maxAtrPct: Double = 3.5,

    val minTrendStrength: Double = 0.22,

    val maxChopScore: Double = 0.68,

    val minLiquidityScore: Double = 0.30
)
