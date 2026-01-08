package com.daemonz.core.analysis

data class CompatibilityScore(
    val score: Int,                // 0..100
    val reasons: List<String>,      // 1-3 dòng ngắn
    val needBacktest: Boolean = true
)