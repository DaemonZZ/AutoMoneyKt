package com.daemonz.runtime

enum class Mode(val label: String, val baseUrl: String) {
    SANDBOX("Sandbox", "https://testnet.binancefuture.com"),
    LIVE("Live", "https://fapi.binance.com")
}