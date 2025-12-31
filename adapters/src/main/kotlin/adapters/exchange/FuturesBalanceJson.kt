package com.daemonz.adapters.exchange

object FuturesBalanceJson {
    fun parse(json: String): List<FuturesBalance> {
        // json thường là array: [{ "asset":"USDT","balance":"...","availableBalance":"..." ...}, ...]
        val out = mutableListOf<FuturesBalance>()
        val items = splitObjects(json)
        for (obj in items) {
            val asset = getStr(obj, "asset") ?: continue
            val wallet = getStr(obj, "balance")?.toDoubleOrNull() ?: 0.0
            val avail = getStr(obj, "availableBalance")?.toDoubleOrNull() ?: 0.0
            val cross = getStr(obj, "crossWalletBalance")?.toDoubleOrNull()
            out += FuturesBalance(asset, wallet, avail, cross)
        }
        return out
    }

    private fun getStr(obj: String, key: String): String? {
        val k = "\"$key\""
        val i = obj.indexOf(k)
        if (i < 0) return null
        val colon = obj.indexOf(':', i)
        if (colon < 0) return null
        val end = obj.indexOfAny(charArrayOf(',', '}'), colon + 1).let { if (it < 0) obj.length else it }
        return obj.substring(colon + 1, end).trim().trim('"')
    }

    // tách các object top-level trong array (thô nhưng chạy)
    private fun splitObjects(json: String): List<String> {
        val s = json.trim()
        if (!s.startsWith("[")) return emptyList()
        val out = mutableListOf<String>()
        var depth = 0
        var start = -1
        for (i in s.indices) {
            when (s[i]) {
                '{' -> {
                    if (depth == 0) start = i; depth++
                }

                '}' -> {
                    depth--
                    if (depth == 0 && start >= 0) {
                        out += s.substring(start, i + 1)
                        start = -1
                    }
                }
            }
        }
        return out
    }
}
