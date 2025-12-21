package com.daemonz.runtime

import com.daemonz.core.trade.Trade

sealed interface RuntimeEvent {
    data class Log(val line: String) : RuntimeEvent
    data class TradeClosed(val trade: Trade) : RuntimeEvent
    data class Status(val symbol: String, val running: Boolean) : RuntimeEvent
}

fun interface EventSink {
    fun emit(e: RuntimeEvent)
}