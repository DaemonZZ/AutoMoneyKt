package com.daemonz.core.analysis

data class CompatibilityScore(
    val score: Int,                // 0..100
    val reasons: List<String>,
    val needBacktest: Boolean = true
)