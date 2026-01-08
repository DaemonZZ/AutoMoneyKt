package com.daemonz.strategies.ema_pullback_v7

data class EmaPullbackV7Params(
    val emaFast: Int = 20,
    val emaSlow: Int = 50,
    val atrPeriod: Int = 14,
    val minPullbackAtr: Double = 0.3,
    val maxPullbackAtr: Double = 1.2,
    val minAtrPct: Double = 0.25,
    val maxAtrPct: Double = 4.50,
    val minTrendStrength: Double = 0.22,
    val maxChopScore: Double = 0.72
)
