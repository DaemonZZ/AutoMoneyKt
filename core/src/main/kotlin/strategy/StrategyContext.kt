package com.daemonz.strategy

import com.daemonz.trade.Position

data class StrategyContext(
    val symbol: String,
    val position: Position,
    val equity: Double,
    val barIndex: Int
)