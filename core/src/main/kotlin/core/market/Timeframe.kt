package com.daemonz.core.market

enum class Timeframe(
    val code: String,
    val label: String
) {
    M1("1m", "1m"),
    M5("5m", "5m"),
    M15("15m", "15m"),
    M30("30m", "30m"),
    H1("1h", "1h"),
    H4("4h", "4h");

    override fun toString(): String = label
}