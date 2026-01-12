package com.daemonz.strategies.ema_pullback_v7

data class EmaPullbackV7CompatibilityParams(
    // Volatility window (ATR/close*100)
    val minAtrPct: Double = 0.25,
    val maxAtrPct: Double = 4.50,

    // Trend/chop regime
    val minTrendStrength: Double = 0.22,
    val maxChopScore: Double = 0.72,

    // Liquidity (nếu Step A chưa compute thì để 1.0 trong stats)
    val minLiquidityScore: Double = 0.30
)
