package com.daemonz.adapters.binance

data class BinanceConfig(
    val apiKey: String,
    val apiSecret: String,
    val baseUrl: String,     // testnet or live
    val recvWindow: Long = 5000
)