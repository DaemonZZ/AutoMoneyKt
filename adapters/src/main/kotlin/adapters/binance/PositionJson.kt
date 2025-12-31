package com.daemonz.adapters.binance

import com.daemonz.adapters.exchange.PositionSnapshot

object PositionJson {
    fun pickFirstPosition(json: String, symbol: String): PositionSnapshot? {
        // positionRisk thường là array; parse thô: tìm entry có "symbol":"XYZ"
        val idx = json.indexOf("\"symbol\":\"$symbol\"")
        if (idx < 0) return null

        fun getStr(key: String): String? {
            val k = "\"$key\""
            val i = json.indexOf(k, idx)
            if (i < 0) return null
            val colon = json.indexOf(':', i)
            val end = json.indexOfAny(charArrayOf(',', '}'), colon + 1).let { if (it < 0) json.length else it }
            return json.substring(colon + 1, end).trim().trim('"')
        }

        val qty = getStr("positionAmt")?.toDoubleOrNull() ?: 0.0
        val entry = getStr("entryPrice")?.toDoubleOrNull() ?: 0.0
        val upnl = getStr("unRealizedProfit")?.toDoubleOrNull() ?: 0.0

        return PositionSnapshot(symbol = symbol, qty = qty, entryPrice = entry, unrealizedPnl = upnl)
    }
}