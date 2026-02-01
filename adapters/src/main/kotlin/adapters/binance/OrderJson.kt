package com.daemonz.adapters.binance

import com.daemonz.adapters.exchange.OrderAck

object OrderJson {
    fun toAck(json: String): OrderAck {
        val id = extractNumber(json, "\"orderId\"") ?: "UNKNOWN"
        return OrderAck(orderId = id, accepted = true)
    }

    private fun extractNumber(json: String, key: String): String? {
        val i = json.indexOf(key)
        if (i < 0) return null
        val colon = json.indexOf(':', i)
        if (colon < 0) return null
        val end = json.indexOfAny(charArrayOf(',', '}'), colon + 1).let { if (it < 0) json.length else it }
        return json.substring(colon + 1, end).trim().trim('"')
    }
}