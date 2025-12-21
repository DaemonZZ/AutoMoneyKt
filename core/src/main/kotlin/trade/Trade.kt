package com.daemonz.trade

enum class Side { LONG, SHORT }

data class Trade(
    val symbol: String,
    val side: Side,
    val qty: Double,
    val entry: Double,
    val exit: Double,
    val entryTime: Long,
    val exitTime: Long,
    val pnl: Double,
    val fees: Double,
    val tag: String? = null
)