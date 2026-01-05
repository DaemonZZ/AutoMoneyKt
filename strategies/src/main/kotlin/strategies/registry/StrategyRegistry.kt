package com.daemonz.strategies.registry

import com.daemonz.core.strategy.StrategyId
import com.daemonz.core.strategy.StrategySpec
import com.daemonz.strategies.ema_pullback_v7.EmaPullbackV7
import com.daemonz.strategies.ema_pullback_v7.EmaPullbackV7Params

object StrategyRegistry {

    val all: List<StrategySpec<*>> = listOf(
        StrategySpec(
            id = StrategyId("ema_pullback_v7"),
            displayName = "EMA Pullback v7",
            defaultParams = { EmaPullbackV7Params() },
            build = { p -> EmaPullbackV7(p) }
        )
        // sau này add tiếp strategy khác ở đây
    )

    fun byId(id: StrategyId): StrategySpec<*>? =
        all.firstOrNull { it.id == id }
}
