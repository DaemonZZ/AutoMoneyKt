package com.daemonz.core.indicator

import com.daemonz.core.market.Candle
import kotlin.math.abs
import kotlin.math.max

/**
 * Streaming ATR (Wilder's smoothing).
 * - update(candle) sequentially.
 * - First ATR uses SMA over 'period' TR values, then Wilder smoothing.
 */
class Atr(private val period: Int) {
    init { require(period > 0) }

    private var prevClose: Double? = null
    private var trCount = 0
    private var trSum = 0.0
    private var atr: Double? = null

    fun reset() {
        prevClose = null
        trCount = 0
        trSum = 0.0
        atr = null
    }

    fun isReady(): Boolean = atr != null
    fun valueOrNull(): Double? = atr

    /**
     * Update ATR with a new candle.
     * @return current ATR if ready, else null during warmup.
     */
    fun update(c: Candle): Double? {
        val pc = prevClose
        val tr = if (pc == null) {
            c.high - c.low
        } else {
            max(
                c.high - c.low,
                max(abs(c.high - pc), abs(c.low - pc))
            )
        }
        prevClose = c.close

        // Warmup ATR by SMA of TR
        if (atr == null) {
            trCount++
            trSum += tr
            if (trCount == period) {
                atr = trSum / period
                return atr
            }
            return null
        }

        // Wilder smoothing: ATR_t = (ATR_{t-1}*(n-1) + TR_t) / n
        atr = ((atr!! * (period - 1)) + tr) / period
        return atr
    }

    fun atrPercent(price: Double): Double? {
        val a = atr ?: return null
        if (price == 0.0) return null
        return a / price
    }
}