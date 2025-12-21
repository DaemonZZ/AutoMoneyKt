package com.daemonz.core.strategy

import com.daemonz.core.market.Candle
import com.daemonz.core.signal.Signal

interface Strategy<P> {
    val name: String

    /** strategy cần tối thiểu bao nhiêu nến để warmup indicator */
    fun warmupBars(params: P): Int = 0

    /** gọi mỗi candle (bar close) hoặc mỗi tick tuỳ runtime, core chỉ biết là event */
    fun onBar(ctx: StrategyContext, bar: Candle, params: P): List<Signal>
}