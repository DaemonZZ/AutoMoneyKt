package com.daemonz.strategies.atr_donchian_breakout_v1

import com.daemonz.core.indicator.Atr
import com.daemonz.core.market.Candle
import com.daemonz.core.signal.Signal
import com.daemonz.core.strategy.Strategy
import com.daemonz.core.trade.Position
import kotlin.math.max

class AtrDonchianBreakoutV1(
    private val params: AtrDonchianBreakoutV1Params = AtrDonchianBreakoutV1Params()
) : Strategy<AtrDonchianBreakoutV1Params> {

    private val lock = Any()

    private val atr = Atr(params.atrLen)
    private val atrSma = AtrSma(params.atrExpansionLen)

    private val candles = ArrayDeque<Candle>() // rolling window
    private val maxKeep = max(params.donchianLen + 5, params.atrLen + 5) + 20

    private var lastCandle: Candle? = null

    // v1.1 state
    private var longArmed = true
    private var shortArmed = true
    private var cooldown = 0

    override fun name(): String = "ATR Donchian Breakout v1.2 (ATR Expansion)"
    override fun defaultParams(): AtrDonchianBreakoutV1Params = params

    override fun onCandle(candle: Candle) {
        synchronized(lock) {
            lastCandle = candle

            candles.addLast(candle)
            if (candles.size > maxKeep) candles.removeFirst()

            // update ATR and ATR-SMA(expansion baseline)
            val a = atr.update(candle)
            if (a != null) {
                atrSma.update(a)
            }

            if (cooldown > 0) cooldown--
        }
    }

    override fun shouldEnter(): Signal? {
        // Snapshot everything we need under lock (prevents CME)
        val snap: List<Candle>
        val atrNow: Double
        val atrMean: Double
        val armedLong: Boolean
        val armedShort: Boolean

        synchronized(lock) {
            if (cooldown > 0) return null
            if (candles.size < params.donchianLen + 1) return null

            snap = candles.toList()

            atrNow = atr.valueOrNull() ?: return null
            atrMean = atrSma.valueOrNull() ?: return null

            armedLong = longArmed
            armedShort = shortArmed
        }

        val last = snap.lastOrNull() ?: return null
        if (last.close <= 0.0) return null

        // ATR% filter
        val atrPct = (atrNow / last.close) * 100.0
        if (atrPct < params.minAtrPct || atrPct > params.maxAtrPct) return null

        // v1.2: ATR Expansion filter
        // Only trade when volatility is expanding, not contracting.
        if (atrNow < atrMean * params.atrExpansionMult) return null

        val upperPrev = highestHighExcludingLast(snap, params.donchianLen)
        val lowerPrev = lowestLowExcludingLast(snap, params.donchianLen)

        // v1.1: channel width filter
        val widthPct = ((upperPrev - lowerPrev) / last.close) * 100.0
        if (widthPct < params.minChannelWidthPct) return null

        // v1.1: re-arm logic when price goes back inside channel
        if (params.rearmOnInsideChannel) {
            synchronized(lock) {
                if (last.close <= upperPrev) longArmed = true
                if (last.close >= lowerPrev) shortArmed = true
            }
        }

        // LONG breakout (one-shot until re-armed)
        if (armedLong && last.close > upperPrev) {
            val strength = last.close - upperPrev
            if (strength >= params.breakoutAtrMult * atrNow) {
                val entry = last.close
                val sl = entry - params.slAtrMult * atrNow
                val slDist = entry - sl
                if (!isSlDistanceOk(entry, slDist)) return null

                val tp = computeTpLong(entry, slDist)

                synchronized(lock) { longArmed = false }

                return Signal.EnterLong(
                    sl = sl,
                    tp = tp,
                    tag = buildString {
                        append("DonchianUp+ATR")
                        append("(str=${fmt(strength)}")
                        append(", width=${fmt(widthPct)}%")
                        append(", atrPct=${fmt(atrPct)}%")
                        append(", atrExp=${fmt(atrNow / atrMean)})")
                    }
                )
            }
        }

        // SHORT breakout (one-shot until re-armed)
        if (armedShort && last.close < lowerPrev) {
            val strength = lowerPrev - last.close
            if (strength >= params.breakoutAtrMult * atrNow) {
                val entry = last.close
                val sl = entry + params.slAtrMult * atrNow
                val slDist = sl - entry
                if (!isSlDistanceOk(entry, slDist)) return null

                val tp = computeTpShort(entry, slDist)

                synchronized(lock) { shortArmed = false }

                return Signal.EnterShort(
                    sl = sl,
                    tp = tp,
                    tag = buildString {
                        append("DonchianDn+ATR")
                        append("(str=${fmt(strength)}")
                        append(", width=${fmt(widthPct)}%")
                        append(", atrPct=${fmt(atrPct)}%")
                        append(", atrExp=${fmt(atrNow / atrMean)})")
                    }
                )
            }
        }

        return null
    }

    override fun shouldExit(position: Position): Signal? {
        val c: Candle = synchronized(lock) {
            lastCandle ?: candles.lastOrNull()
        } ?: return null

        val exitSignal: Signal? = when (position) {
            is Position.Flat -> null

            is Position.LongPos -> {
                // Long: SL hit if low <= sl; TP hit if high >= tp
                // If both hit same candle -> SL first (conservative)
                val slHit = c.low <= position.sl
                val tpHit = c.high >= position.tp
                when {
                    slHit -> Signal.Exit
                    tpHit -> Signal.Exit
                    else -> null
                }
            }

            is Position.ShortPos -> {
                // Short: SL hit if high >= sl; TP hit if low <= tp
                // If both hit same candle -> SL first (conservative)
                val slHit = c.high >= position.sl
                val tpHit = c.low <= position.tp
                when {
                    slHit -> Signal.Exit
                    tpHit -> Signal.Exit
                    else -> null
                }
            }
        }

        if (exitSignal == Signal.Exit) {
            synchronized(lock) {
                cooldown = params.cooldownBarsAfterExit.coerceAtLeast(0)
                longArmed = true
                shortArmed = true
            }
        }

        return exitSignal
    }

    // ---- Donchian helpers on snapshot (NO shared mutation) ----

    private fun highestHighExcludingLast(snap: List<Candle>, len: Int): Double {
        var maxHigh = Double.NEGATIVE_INFINITY
        val end = snap.size - 2 // exclude last
        val start = maxOf(0, end - len + 1)
        for (i in start..end) {
            val h = snap[i].high
            if (h > maxHigh) maxHigh = h
        }
        return maxHigh
    }

    private fun lowestLowExcludingLast(snap: List<Candle>, len: Int): Double {
        var minLow = Double.POSITIVE_INFINITY
        val end = snap.size - 2 // exclude last
        val start = maxOf(0, end - len + 1)
        for (i in start..end) {
            val l = snap[i].low
            if (l < minLow) minLow = l
        }
        return minLow
    }

    private fun computeTpLong(entry: Double, slDist: Double): Double {
        return if (params.tpR > 0.0) entry + params.tpR * slDist else entry + 1e12
    }

    private fun computeTpShort(entry: Double, slDist: Double): Double {
        return if (params.tpR > 0.0) entry - params.tpR * slDist else entry - 1e12
    }

    private fun isSlDistanceOk(entry: Double, slDist: Double): Boolean {
        if (slDist <= 0.0) return false
        val minDist = entry * (params.minSlDistancePct / 100.0)
        return slDist >= minDist
    }

    private fun fmt(v: Double): String = "%,.4f".format(v)

    // ---- ATR SMA helper (streaming) ----
    private class AtrSma(private val period: Int) {
        init {
            require(period > 0)
        }

        private val buf = ArrayDeque<Double>()
        private var sum = 0.0

        fun reset() {
            buf.clear()
            sum = 0.0
        }

        fun update(v: Double): Double? {
            buf.addLast(v)
            sum += v
            if (buf.size > period) {
                sum -= buf.removeFirst()
            }
            return valueOrNull()
        }

        fun valueOrNull(): Double? {
            return if (buf.size == period) sum / period else null
        }
    }
}
