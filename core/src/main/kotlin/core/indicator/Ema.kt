package com.daemonz.core.indicator

class Ema(private val period: Int) {
    init { require(period > 0) }

    private val alpha = 2.0 / (period + 1.0)
    private var count = 0
    private var sum = 0.0
    private var ema: Double? = null

    fun reset() {
        count = 0
        sum = 0.0
        ema = null
    }

    fun isReady(): Boolean = ema != null
    fun valueOrNull(): Double? = ema

    fun update(price: Double): Double? {
        count++

        if (ema == null) {
            sum += price
            if (count == period) ema = sum / period
            return ema
        }

        val prev = ema ?: return null // defensive, practically never hits
        ema = (price - prev) * alpha + prev
        return ema
    }
}
