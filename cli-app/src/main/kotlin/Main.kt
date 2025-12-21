package com.daemonz

import com.daemonz.engine.BacktestEngine
import com.daemonz.market.Candle
import com.daemonz.strategy.demo.DemoParams
import com.daemonz.strategy.demo.EmaAtrDemoStrategy
import kotlin.random.Random

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    val candles = generateFakeCandles(800)

    val params = DemoParams(
        emaFast = 20,
        emaSlow = 50,
        atrPeriod = 14,
        minAtrPct = 0.0015,
        tpAtr = 2.0,
        slAtr = 1.5
    )

    val strategy = EmaAtrDemoStrategy(params)
    val engine = BacktestEngine()

    val result = engine.run(
        symbol = "BTCUSDT",
        candles = candles,
        strategy = strategy,
        params = params
    )

    println("Strategy: ${strategy.name}")
    println("Trades: ${result.trades.size}")
    println("Ending Equity: ${"%.2f".format(result.endingEquity)}")
    println("Sample trades:")
    result.trades.take(5).forEach { println(it) }
}

/** Fake OHLCV để test core */
private fun generateFakeCandles(n: Int): List<Candle> {
    val rnd = Random(42)
    val out = ArrayList<Candle>(n)
    var price = 100.0
    var t = 0L

    repeat(n) {
        val move = (rnd.nextDouble() - 0.5) * 1.2
        val open = price
        price = (price + move).coerceAtLeast(1.0)
        val close = price
        val high = maxOf(open, close) + rnd.nextDouble() * 0.4
        val low = minOf(open, close) - rnd.nextDouble() * 0.4

        out += Candle(
            t = t,
            open = open,
            high = high,
            low = low,
            close = close,
            volume = rnd.nextDouble() * 1000
        )
        t += 60_000L
    }
    return out
}