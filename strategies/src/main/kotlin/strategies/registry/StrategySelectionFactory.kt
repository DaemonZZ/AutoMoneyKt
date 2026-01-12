package com.daemonz.strategies.registry

import com.daemonz.core.strategy.StrategySpec

@Suppress("UNCHECKED_CAST")
fun StrategySpec<*>.newSelectionAny(): StrategySelection {
    // Cast sang StrategySpec<Any> chỉ để gọi generic helper newSelection()
    return (this as StrategySpec<Any>).newSelection()
}
