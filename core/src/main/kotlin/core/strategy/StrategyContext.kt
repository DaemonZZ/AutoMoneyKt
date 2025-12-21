package com.daemonz.core.strategy

import com.daemonz.core.trade.Position

data class StrategyContext(
    val symbol: String,
    val position: Position,
    val equity: Double,
    val barIndex: Int
)