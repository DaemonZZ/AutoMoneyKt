package com.daemonz.core.analysis

data class EligibilityThresholds(
    val minAtrPct: Double = 0.15,
    val maxAtrPct: Double = 3.50,
    val minTrendStrength: Double = 0.20,
    val maxChopScore: Double = 0.70,

    // For regime labeling (not strict filtering)
    val trendChopUpper: Double = 0.55,
    val chopHard: Double = 0.75
)