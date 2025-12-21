package com.daemonz.core.strategy.demo

import com.daemonz.core.indicator.Atr
import com.daemonz.core.indicator.Ema
import com.daemonz.core.market.Candle
import com.daemonz.core.signal.Signal
import com.daemonz.core.strategy.Strategy
import com.daemonz.core.strategy.StrategyContext
import com.daemonz.core.trade.Position
import kotlin.math.abs

data class DemoParams(
    val emaFast: Int = 20,
    val emaSlow: Int = 50,
    val atrPeriod: Int = 14,
    val minAtrPct: Double = 0.002,     // 0.2%
    val tpAtr: Double = 2.0,
    val slAtr: Double = 1.5
)

class EmaAtrDemoStrategy(params: DemoParams) : Strategy<DemoParams> {
    override val name: String = "EMA+ATR Demo"

    private val fast = Ema(params.emaFast)
    private val slow = Ema(params.emaSlow)
    private val atr = Atr(params.atrPeriod)

    override fun warmupBars(params: DemoParams): Int =
        maxOf(params.emaFast, params.emaSlow, params.atrPeriod) + 5

    override fun onBar(ctx: StrategyContext, bar: Candle, params: DemoParams): List<Signal> {
        val eFast = fast.update(bar.close)
        val eSlow = slow.update(bar.close)
        val a = atr.update(bar)

        // chưa đủ data
        if (eFast == null || eSlow == null || a == null) return listOf(Signal.NoOp)

        val atrPct = atr.atrPercent(bar.close) ?: return listOf(Signal.NoOp)
        if (atrPct < params.minAtrPct) return listOf(Signal.NoOp)

        return when (ctx.position) {
            is Position.Flat -> {
                // simple cross demo
                if (eFast > eSlow) {
                    val sl = bar.close - params.slAtr * a
                    val tp = bar.close + params.tpAtr * a
                    listOf(Signal.EnterLong(sl = sl, tp = tp, tag = "ema_cross"))
                } else if (eFast < eSlow) {
                    val sl = bar.close + params.slAtr * a
                    val tp = bar.close - params.tpAtr * a
                    listOf(Signal.EnterShort(sl = sl, tp = tp, tag = "ema_cross"))
                } else listOf(Signal.NoOp)
            }
            else -> {
                // optional: exit on mean reversion (demo)
                val dist = abs(bar.close - eSlow)
                if (dist < 0.2 * a) listOf(Signal.Exit) else listOf(Signal.NoOp)
            }
        }
    }
}