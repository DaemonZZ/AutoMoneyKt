package com.daemonz.core.signal

sealed interface Signal {
    data class EnterLong(
        val sl: Double,
        val tp: Double,
        val tag: String? = null
    ) : Signal

    data class EnterShort(
        val sl: Double,
        val tp: Double,
        val tag: String? = null
    ) : Signal

    object Exit : Signal

    data class UpdateSLTP(
        val sl: Double? = null,
        val tp: Double? = null
    ) : Signal

    object NoOp : Signal
}