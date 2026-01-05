package com.daemonz.core.strategy

import com.daemonz.core.market.Candle
import com.daemonz.core.signal.Signal
import com.daemonz.core.trade.Position

interface Strategy<P> {
    fun name(): String
    fun defaultParams(): P

    fun onCandle(candle: Candle)
    fun shouldEnter(): Signal?
    fun shouldExit(position: Position): Signal?
}