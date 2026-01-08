package com.daemonz.adapters.exchange

import com.daemonz.core.market.Candle
import com.daemonz.core.market.Ticker24h
import com.daemonz.core.market.Timeframe


interface ExchangeAdapter {
    fun name(): String

    fun getSymbolInfo(symbol: String): SymbolInfo

    /** For paper/live: latest candles fetch. For live could be websocket cache. */
    fun fetchCandles(symbol: String, limit: Int): List<Candle>

    fun getPosition(symbol: String): PositionSnapshot?

    fun placeOrder(req: PlaceOrderRequest): OrderAck

    fun closePositionMarket(symbol: String): OrderAck

    /** Kiểm tra kết nối + quyền API cơ bản */
    fun checkConnection(): ConnectionStatus

    /** Lấy số dư Futures (USDT-M) */
    fun fetchFuturesBalances(): List<FuturesBalance>

    fun fetchFuturesAccountInfo(): FuturesAccountInfo
    fun listTradableSymbols(): List<String>   // Futures USDT-M, trạng thái TRADING
    fun fetchCandles(symbol: String, interval: Timeframe, limit: Int): List<Candle>
    fun fetchTickers24h(): List<Ticker24h>

    /**
     * Return candles in ascending time order (oldest -> newest).
     * windowBars: number of bars needed.
     */
    suspend fun getKlines(symbol: String, timeframe: Timeframe, windowBars: Int): List<Candle>
}