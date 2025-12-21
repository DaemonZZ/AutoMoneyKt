package com.daemonz.trade

sealed interface Position {
    object Flat : Position

    data class LongPos(
        val entryPrice: Double,
        val qty: Double,
        val sl: Double,
        val tp: Double,
        val entryTime: Long
    ) : Position

    data class ShortPos(
        val entryPrice: Double,
        val qty: Double,
        val sl: Double,
        val tp: Double,
        val entryTime: Long
    ) : Position
}