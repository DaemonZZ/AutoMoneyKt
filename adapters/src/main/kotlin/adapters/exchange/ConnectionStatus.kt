package com.daemonz.adapters.exchange

class ConnectionStatus(
    val ok: Boolean,
    val message: String,
    val serverTimeMs: Long? = null
)