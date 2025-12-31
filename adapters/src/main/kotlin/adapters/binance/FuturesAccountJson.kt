package com.daemonz.adapters.binance

import com.daemonz.adapters.exchange.FuturesAccountInfo

object FuturesAccountJson {
    fun parse(json: String): FuturesAccountInfo {
        fun getBool(key: String): Boolean =
            getStr(json, key)?.lowercase() == "true"

        fun getDouble(key: String): Double? =
            getStr(json, key)?.toDoubleOrNull()

        fun getLong(key: String): Long? =
            getStr(json, key)?.toLongOrNull()

        return FuturesAccountInfo(
            canTrade = getBool("canTrade"),
            canWithdraw = getBool("canWithdraw"),
            canDeposit = getBool("canDeposit"),
            multiAssetsMargin = getStr(json, "multiAssetsMargin")?.lowercase()?.let { it == "true" },
            totalWalletBalance = getDouble("totalWalletBalance"),
            totalAvailableBalance = getDouble("availableBalance") ?: getDouble("totalAvailableBalance"),
            totalUnrealizedProfit = getDouble("totalUnrealizedProfit"),
            updateTimeMs = getLong("updateTime")
        )
    }

    private fun getStr(src: String, key: String): String? {
        val k = "\"$key\""
        val i = src.indexOf(k)
        if (i < 0) return null
        val colon = src.indexOf(':', i)
        if (colon < 0) return null
        val end = src.indexOfAny(charArrayOf(',', '}'), colon + 1).let { if (it < 0) src.length else it }
        return src.substring(colon + 1, end).trim().trim('"')
    }
}
