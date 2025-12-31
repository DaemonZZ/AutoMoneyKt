package com.daemonz.models

import com.daemonz.adapters.exchange.FuturesAccountInfo
import com.daemonz.adapters.exchange.FuturesBalance

data class AccountSnapshot(
    val connectionOk: Boolean,
    val message: String,
    val accountInfo: FuturesAccountInfo? = null,
    val balances: List<FuturesBalance> = emptyList()
)
