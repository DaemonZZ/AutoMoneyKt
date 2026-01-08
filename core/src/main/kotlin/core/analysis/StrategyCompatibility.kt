package com.daemonz.core.analysis

interface StrategyCompatibility<P> {
    fun compatibility(stats: MarketStats, params: P): CompatibilityScore
}