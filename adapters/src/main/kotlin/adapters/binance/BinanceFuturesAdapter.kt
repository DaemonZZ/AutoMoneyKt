package com.daemonz.adapters.binance

import com.daemonz.adapters.exchange.*
import com.daemonz.core.market.Candle
import com.daemonz.core.market.Ticker24h
import com.daemonz.core.market.Timeframe
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.time.Instant
import java.util.concurrent.TimeUnit
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

    private val http = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun fetchTickers24h(): List<Ticker24h> {
        val url = "${cfg.baseUrl}/fapi/v1/ticker/24hr"
        val req = Request.Builder().url(url).get().build()

        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val body = resp.body?.string().orEmpty()
                throw IllegalStateException("ticker24h failed: ${resp.code} $body")
            }

            val arr = JSONArray(resp.body?.string() ?: "[]")
            val out = ArrayList<Ticker24h>(arr.length())

            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val symbol = o.optString("symbol")
                val quoteVol = o.optString("quoteVolume", "0").toDoubleOrNull() ?: 0.0
                val chgPct = o.optString("priceChangePercent", "0").toDoubleOrNull() ?: 0.0

                // USDT-M futures tickers vẫn trả đủ, bạn lọc symbol ở runtime
                out.add(Ticker24h(symbol, quoteVol, chgPct))
            }
            return out
        }
    }

    override fun listTradableSymbols(): List<String> {
        val url = "${cfg.baseUrl}/fapi/v1/exchangeInfo"
        val req = Request.Builder().url(url).get().build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                error("exchangeInfo failed: ${resp.code}")
            }

            val json = org.json.JSONObject(resp.body!!.string())
            val symbols = json.getJSONArray("symbols")

            val result = mutableListOf<String>()
            for (i in 0 until symbols.length()) {
                val s = symbols.getJSONObject(i)
                if (
                    s.getString("status") == "TRADING" &&
                    s.getString("quoteAsset") == "USDT"
                ) {
                    result += s.getString("symbol")
                }
            }
            return result
        }
    }

    override fun fetchCandles(
        symbol: String,
        interval: Timeframe,
        limit: Int
    ): List<Candle> {

        val url =
            "${cfg.baseUrl}/fapi/v1/klines" +
                    "?symbol=$symbol" +
                    "&interval=$interval" +
                    "&limit=$limit"

        val req = Request.Builder().url(url).get().build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                error("klines failed for $symbol: ${resp.code}")
            }

            val arr = JSONArray(resp.body!!.string())
            val candles = ArrayList<Candle>(arr.length())

            for (i in 0 until arr.length()) {
                val k = arr.getJSONArray(i)

                candles += Candle(
                    t = k.getLong(0),                // open time
                    open = k.getString(1).toDouble(),
                    high = k.getString(2).toDouble(),
                    low = k.getString(3).toDouble(),
                    close = k.getString(4).toDouble(),
                    volume = k.getString(5).toDouble()
                )
            }

            return candles
        }
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