package com.daemonz.core.market

data class Ticker24h(
    val symbol: String,
    val quoteVolume: Double,
    val priceChangePercent: Double
)