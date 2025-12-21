package com.daemonz.adapters.exchange

import com.daemonz.core.market.Candle
import kotlin.random.Random

class FakeExchangeAdapter(
    private val seed: Int = 42
) : ExchangeAdapter {

    private val rnd = Random(seed)
    private val market = mutableMapOf<String, MutableList<Candle>>()
    private val positions = mutableMapOf<String, PositionSnapshot>()

    override fun name(): String = "FakeExchangeAdapter"

    override fun getSymbolInfo(symbol: String): SymbolInfo =
        SymbolInfo(symbol, tickSize = 0.01, stepSize = 0.001)

    override fun fetchCandles(symbol: String, limit: Int): List<Candle> {
        val list = market.getOrPut(symbol) { generate(symbol, 2000).toMutableList() }
        return list.takeLast(limit)
    }

    override fun getPosition(symbol: String): PositionSnapshot? = positions[symbol]

    override fun placeOrder(req: PlaceOrderRequest): OrderAck {
        // paper stub: accept but do not simulate fill here (runtime/paper engine handles fills)
        return OrderAck(orderId = "FAKE-${System.nanoTime()}", accepted = true)
    }

    override fun closePositionMarket(symbol: String): OrderAck {
        positions.remove(symbol)
        return OrderAck(orderId = "FAKE-CLOSE-${System.nanoTime()}", accepted = true)
    }

    private fun generate(symbol: String, n: Int): List<Candle> {
        val out = ArrayList<Candle>(n)
        var price = 100.0 + rnd.nextDouble() * 10
        var t = 0L
        repeat(n) {
            val move = (rnd.nextDouble() - 0.5) * 1.2
            val open = price
            price = (price + move).coerceAtLeast(1.0)
            val close = price
            val high = maxOf(open, close) + rnd.nextDouble() * 0.4
            val low = minOf(open, close) - rnd.nextDouble() * 0.4
            out += Candle(t, open, high, low, close, volume = rnd.nextDouble() * 1000)
            t += 60_000L
        }
        return out
    }
}