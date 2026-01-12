package com.daemonz.strategies.ema_pullback_v7

data class EmaPullbackV7Params(
    val emaFast: Int = 20,
    val emaSlow: Int = 50,
    val atrPeriod: Int = 14,

    // trade logic: pullback window in ATR
    val minPullbackAtr: Double = 0.3,
    val maxPullbackAtr: Double = 1.2
)
