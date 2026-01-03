package com.daemonz.adapters.binance

import com.daemonz.adapters.exchange.ConnectionStatus
import com.daemonz.adapters.exchange.ExchangeAdapter
import com.daemonz.adapters.exchange.FuturesAccountInfo
import com.daemonz.adapters.exchange.FuturesBalance
import com.daemonz.adapters.exchange.FuturesBalanceJson
import com.daemonz.adapters.exchange.OrderAck
import com.daemonz.adapters.exchange.OrderSide
import com.daemonz.adapters.exchange.OrderType
import com.daemonz.adapters.exchange.PlaceOrderRequest
import com.daemonz.adapters.exchange.PositionSnapshot
import com.daemonz.adapters.exchange.SymbolInfo
import com.daemonz.core.market.Candle
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import kotlin.math.abs

class BinanceFuturesAdapter(
    private val cfg: BinanceConfig,
    private val client: OkHttpClient = OkHttpClient()
) : ExchangeAdapter {
    override fun name() = "BinanceFutures(${cfg.baseUrl})"

    override fun getSymbolInfo(symbol: String): SymbolInfo {
        return SymbolInfo(symbol = symbol, tickSize = 0.01, stepSize = 0.001)
    }

    override fun fetchFuturesAccountInfo(): FuturesAccountInfo {
        val json = signedGet("/fapi/v2/account", emptyMap())
        return FuturesAccountJson.parse(json)
    }

    override fun listTradableSymbols(): List<String> {
        TODO("Not yet implemented")
    }

    override fun fetchCandles(
        symbol: String,
        interval: String,
        limit: Int
    ): List<Candle> {
        TODO("Not yet implemented")
    }

    override fun fetchCandles(
        symbol: String,
        limit: Int
    ): List<Candle> {
        // Futures klines endpoint (thường là /fapi/v1/klines)
        val url = (cfg.baseUrl + "/fapi/v1/klines").toHttpUrl().newBuilder()
            .addQueryParameter("symbol", symbol)
            .addQueryParameter("interval", "1m")
            .addQueryParameter("limit", limit.coerceIn(1, 1500).toString())
            .build()

        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            require(resp.isSuccessful) { "fetchCandles failed: ${resp.code} ${resp.body?.string()}" }
            val body = resp.body?.string() ?: "[]"

            // Body là mảng mảng: [ [openTime, open, high, low, close, volume, closeTime, ...], ... ]
            // Parse nhanh gọn không dùng serializer để tránh setup nặng: làm thủ công đơn giản.
            return KlinesJson.parse(body)
        }
    }

    override fun getPosition(symbol: String): PositionSnapshot? {
        val json = signedGet("/fapi/v2/positionRisk", mapOf("symbol" to symbol))
        return PositionJson.pickFirstPosition(json, symbol)
    }

    override fun placeOrder(req: PlaceOrderRequest): OrderAck {
        // order endpoint (thường: /fapi/v1/order)
        val params = linkedMapOf(
            "symbol" to req.symbol,
            "side" to req.side.name,
            "type" to req.type.name,
            "quantity" to req.qty.toString()
        )
        if (req.price != null) params["price"] = req.price.toString()
        if (req.reduceOnly) params["reduceOnly"] = "true"

        val json = signedPost("/fapi/v1/order", params)
        return OrderJson.toAck(json)
    }

    override fun closePositionMarket(symbol: String): OrderAck {
        val pos = getPosition(symbol) ?: return OrderAck("N/A", accepted = true, message = "No position")
        if (pos.qty == 0.0) return OrderAck("N/A", accepted = true, message = "No position")

        val side = if (pos.qty > 0) OrderSide.SELL else OrderSide.BUY
        val qty = abs(pos.qty)

        // reduceOnly=true để đóng vị thế (với Futures).
        return placeOrder(
            PlaceOrderRequest(
                symbol = symbol,
                side = side,
                type = OrderType.MARKET,
                qty = qty,
                reduceOnly = true
            )
        )
    }

    override fun checkConnection(): ConnectionStatus {
        // 1) ping (public)
        try {
            val pingUrl = (cfg.baseUrl + "/fapi/v1/ping").toHttpUrl()
            val pingReq = Request.Builder().url(pingUrl).get().build()
            client.newCall(pingReq).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return ConnectionStatus(false, "Ping failed: ${resp.code}")
                }
            }
        } catch (t: Throwable) {
            return ConnectionStatus(false, "Ping exception: ${t.message}")
        }

        // 2) signed account check (private)
        return try {
            val json = signedGet("/fapi/v2/account", emptyMap())
            // nếu tới được đây là key/secret ok
            ConnectionStatus(true, "OK", serverTimeMs = extractLong(json, "\"updateTime\""))
        } catch (t: Throwable) {
            ConnectionStatus(false, "Auth failed: ${t.message}")
        }
    }

    override fun fetchFuturesBalances(): List<FuturesBalance> {
        val json = signedGet("/fapi/v2/balance", emptyMap())
        return FuturesBalanceJson.parse(json)
    }

    private fun signedGet(path: String, params: Map<String, String>): String {
        val ts = Instant.now().toEpochMilli().toString()
        val all = LinkedHashMap<String, String>()
        all.putAll(params)
        all["recvWindow"] = cfg.recvWindow.toString()
        all["timestamp"] = ts

        val query = all.entries.joinToString("&") { "${it.key}=${it.value}" }
        val sig = BinanceSigner.hmacSha256Hex(cfg.apiSecret, query)

        val url = (cfg.baseUrl + path).toHttpUrl().newBuilder()
            .encodedQuery("$query&signature=$sig")
            .build()

        val req = Request.Builder()
            .url(url)
            .addHeader("X-MBX-APIKEY", cfg.apiKey)
            .get()
            .build()

        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string()
            require(resp.isSuccessful) { "GET $path failed: ${resp.code} $body" }
            return body ?: "{}"
        }
    }

    private fun signedPost(path: String, params: Map<String, String>): String {
        val ts = Instant.now().toEpochMilli().toString()
        val all = LinkedHashMap<String, String>()
        all.putAll(params)
        all["recvWindow"] = cfg.recvWindow.toString()
        all["timestamp"] = ts

        val query = all.entries.joinToString("&") { "${it.key}=${it.value}" }
        val sig = BinanceSigner.hmacSha256Hex(cfg.apiSecret, query)

        val url = (cfg.baseUrl + path).toHttpUrl()
        val body = ("$query&signature=$sig").toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())

        val req = Request.Builder()
            .url(url)
            .addHeader("X-MBX-APIKEY", cfg.apiKey)
            .post(body)
            .build()

        client.newCall(req).execute().use { resp ->
            val respBody = resp.body?.string()
            require(resp.isSuccessful) { "POST $path failed: ${resp.code} $respBody" }
            return respBody ?: "{}"
        }
    }

    private fun extractLong(json: String, key: String): Long? {
        val i = json.indexOf(key)
        if (i < 0) return null
        val colon = json.indexOf(':', i)
        if (colon < 0) return null
        val end = json.indexOfAny(charArrayOf(',', '}'), colon + 1).let { if (it < 0) json.length else it }
        return json.substring(colon + 1, end).trim().trim('"').toLongOrNull()
    }
}