package com.daemonz.runtime.scanner

data class StepBConfig(
    val maxConcurrency: Int = 6,
    val applyFilters: Boolean = true,

    // score blending
    val weightMarket: Double = 0.4,
    val weightCompat: Double = 0.6,

    // compat thresholds (labeling)
    val compatTradeThreshold: Int = 70,
    val compatWatchThreshold: Int = 50,

    // backtest gate
    val minFinalScoreForBacktest: Int = 40,

    // debug
    val debugLogs: Boolean = true
)