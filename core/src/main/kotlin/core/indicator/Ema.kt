package com.daemonz.core.indicator

/**
 * Streaming EMA.
 * - Call update(price) sequentially per bar close.
 * - First EMA value uses SMA warmup over 'period' samples.
 */
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

    fun period(): Int = period
    fun isReady(): Boolean = ema != null
    fun valueOrNull(): Double? = ema

    /**
     * Update EMA with a new price.
     * @return current EMA value if ready, else null during warmup.
     */
    fun update(price: Double): Double? {
        count++

        // Warmup: build SMA for first 'period' points.
        if (ema == null) {
            sum += price
            if (count == period) {
                ema = sum / period
                return ema
            }
            return null
        }

        // Streaming EMA update
        ema = (price - ema!!) * alpha + ema!!
        return ema
    }
}