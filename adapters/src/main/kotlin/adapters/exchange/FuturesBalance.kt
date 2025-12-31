package com.daemonz.adapters.exchange

data class FuturesBalance(
    val asset: String,          // e.g. USDT
    val walletBalance: Double,  // ví
    val availableBalance: Double, // có thể dùng
    val crossWalletBalance: Double? = null
)