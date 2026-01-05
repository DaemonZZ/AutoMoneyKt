package com.daemonz.core.market

enum class Timeframe(
    val code: String,     // dùng cho exchange (Binance)
    val label: String     // dùng cho UI
) {
    M5("5m", "5m"),
    M15("15m", "15m"),
    H1("1h", "1h"),
    H4("4h", "4h");

    override fun toString(): String = label
}