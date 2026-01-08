package com.daemonz.core.analysis

data class MarketStats(
    val atrPct: Double,         // ATR / close * 100
    val trendStrength: Double,  // 0..1 (abs EMA slope normalized by ATR)
    val chopScore: Double,      // 0..1 (higher = more choppy)
    val liquidityScore: Double  // 0..1 (step A can default to 1.0)
)