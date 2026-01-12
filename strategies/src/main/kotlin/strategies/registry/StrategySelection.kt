package com.daemonz.strategies.registry

import com.daemonz.core.strategy.Strategy
import com.daemonz.core.strategy.StrategyId
import com.daemonz.core.strategy.StrategySpec

sealed interface StrategySelection {
    val id: StrategyId
    val displayName: String

    fun paramsAny(): Any
    fun setParamsAny(newParams: Any)

    fun buildStrategyAny(): Strategy<Any>
}

class StrategySelectionImpl<P>(
    private val spec: StrategySpec<P>,
    private var params: P
) : StrategySelection {

    override val id: StrategyId get() = spec.id
    override val displayName: String get() = spec.displayName

    override fun paramsAny(): Any = params as Any

    @Suppress("UNCHECKED_CAST")
    override fun setParamsAny(newParams: Any) {
        params = newParams as P
    }

    @Suppress("UNCHECKED_CAST")
    override fun buildStrategyAny(): Strategy<Any> {
        return spec.build(params) as Strategy<Any>
    }

    fun paramsTyped(): P = params
    fun buildStrategyTyped(): Strategy<P> = spec.build(params)
}

fun <P> StrategySpec<P>.newSelection(initialParams: P = defaultParams()): StrategySelectionImpl<P> {
    return StrategySelectionImpl(this, initialParams)
}
