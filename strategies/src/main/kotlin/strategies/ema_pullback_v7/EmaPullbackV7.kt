package com.daemonz.strategies.ema_pullback_v7

import com.daemonz.core.indicator.Atr
import com.daemonz.core.indicator.Ema
import com.daemonz.core.market.Candle
import com.daemonz.core.signal.Signal
import com.daemonz.core.strategy.Strategy
import com.daemonz.core.trade.Position
import kotlin.math.abs

class EmaPullbackV7(
    private val params: EmaPullbackV7Params
) : Strategy<EmaPullbackV7Params> {

    override fun name() = "EMA Pullback v7"

    override fun defaultParams() = params

    // indicators (streaming)
    private val emaFast = Ema(params.emaFast)
    private val emaSlow = Ema(params.emaSlow)
    private val atr = Atr(params.atrPeriod)

    private var lastClose: Double? = null

    override fun onCandle(c: Candle) {
        lastClose = c.close
        emaFast.update(c.close)
        emaSlow.update(c.close)
        atr.update(c)
    }

    override fun shouldEnter(): Signal {
        val close = lastClose ?: return Signal.NoOp

        val fast = emaFast.valueOrNull() ?: return Signal.NoOp
        val slow = emaSlow.valueOrNull() ?: return Signal.NoOp
        val atrv = atr.valueOrNull() ?: return Signal.NoOp

        val pullback = abs(fast - slow) / atrv
        if (pullback !in params.minPullbackAtr..params.maxPullbackAtr) return Signal.NoOp

        val sl: Double
        val tp: Double

        if (fast > slow) {
            sl = close - atrv * 1.5
            tp = close + atrv * 3.0
            return Signal.EnterLong(sl, tp, "EMA pullback")
        } else {
            sl = close + atrv * 1.5
            tp = close - atrv * 3.0
            return Signal.EnterShort(sl, tp, "EMA pullback")
        }
    }

    override fun shouldExit(position: Position): Signal {
        val fast = emaFast.valueOrNull() ?: return Signal.NoOp
        val slow = emaSlow.valueOrNull() ?: return Signal.NoOp

        return when (position) {
            is Position.LongPos ->
                if (fast < slow) Signal.Exit else Signal.NoOp

            is Position.ShortPos ->
                if (fast > slow) Signal.Exit else Signal.NoOp

            Position.Flat ->
                Signal.NoOp
        }
    }
}
