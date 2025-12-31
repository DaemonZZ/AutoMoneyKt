package com.daemonz.adapters.exchange

import com.daemonz.core.market.Candle


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

}