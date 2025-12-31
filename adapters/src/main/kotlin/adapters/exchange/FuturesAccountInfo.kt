package com.daemonz.adapters.exchange

data class FuturesAccountInfo(
    val canTrade: Boolean,
    val canWithdraw: Boolean,
    val canDeposit: Boolean,
    val multiAssetsMargin: Boolean? = null,
    val totalWalletBalance: Double? = null,
    val totalAvailableBalance: Double? = null,
    val totalUnrealizedProfit: Double? = null,
    val updateTimeMs: Long? = null
)