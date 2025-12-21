package com.daemonz.adapters.exchange

data class SymbolInfo(
    val symbol: String,
    val tickSize: Double,
    val stepSize: Double
)

enum class OrderSide { BUY, SELL }
enum class OrderType { MARKET, LIMIT }

data class PlaceOrderRequest(
    val symbol: String,
    val side: OrderSide,
    val type: OrderType,
    val qty: Double,
    val price: Double? = null,
    val reduceOnly: Boolean = false
)

data class OrderAck(
    val orderId: String,
    val accepted: Boolean,
    val message: String? = null
)

data class PositionSnapshot(
    val symbol: String,
    val qty: Double,      // +long, -short
    val entryPrice: Double,
    val unrealizedPnl: Double
)