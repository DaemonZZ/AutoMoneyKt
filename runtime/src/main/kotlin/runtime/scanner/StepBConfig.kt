package com.daemonz.runtime.scanner

data class StepBConfig(
    val maxConcurrency: Int = 6,
    val applyFilters: Boolean = true,     // giống Step A
    val compatThreshold: Int = 60,        // >= thì TRADE
    val weightMarket: Double = 0.40,      // trọng số Step A confidence
    val weightCompat: Double = 0.60       // trọng số Step B compatibility
)