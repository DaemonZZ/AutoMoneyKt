package com.daemonz.adapters.binance

import com.daemonz.core.market.Candle

object KlinesJson {
    fun parse(json: String): List<Candle> {
        // json: [[openTime,"open","high","low","close","volume",...],...]
        val trimmed = json.trim()
        if (trimmed.length < 5) return emptyList()

        val out = mutableListOf<Candle>()
        // Tách từng kline block bằng "],["
        val inner = trimmed.removePrefix("[").removeSuffix("]")
        if (inner.isBlank()) return emptyList()

        val chunks = inner.split("],[")
        for (c in chunks) {
            val row = c.replace("[", "").replace("]", "")
            val cols = row.split(",")
            if (cols.size < 6) continue

            val t = cols[0].trim().toLong()
            val open = cols[1].trim().trim('"').toDouble()
            val high = cols[2].trim().trim('"').toDouble()
            val low = cols[3].trim().trim('"').toDouble()
            val close = cols[4].trim().trim('"').toDouble()
            val vol = cols[5].trim().trim('"').toDouble()

            out += Candle(t = t, open = open, high = high, low = low, close = close, volume = vol)
        }
        return out
    }
}