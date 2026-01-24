package com.daemonz.runtime

data class BotSpec(
    val symbol: String,
    val timeframe: String = "1m",
    val mode: Mode = Mode.SANDBOX
)