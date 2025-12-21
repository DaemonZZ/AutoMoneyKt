package com.daemonz.market

data class Candle(
    val t: Long,            // epoch millis
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double = 0.0
)