package com.daemonz.core.strategy

import com.daemonz.core.strategy.Strategy

@JvmInline
value class StrategyId(val value: String)

data class StrategySpec<P>(
    val id: StrategyId,
    val displayName: String,
    val defaultParams: () -> P,
    val build: (P) -> Strategy<P>
)