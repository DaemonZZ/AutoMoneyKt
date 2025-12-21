package com.daemonz

import com.daemonz.adapters.exchange.FakeExchangeAdapter
import com.daemonz.core.market.Candle
import com.daemonz.runtime.BotRunner
import com.daemonz.runtime.BotSpec
import com.daemonz.runtime.EventSink
import com.daemonz.runtime.RunMode
import com.daemonz.runtime.RuntimeEvent
import com.daemonz.core.strategy.demo.DemoParams
import com.daemonz.core.strategy.demo.EmaAtrDemoStrategy
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main(args: Array<String>) {
    val symbol = args.firstOrNull() ?: "BTCUSDT"

    val exchange = FakeExchangeAdapter()
    val sink = EventSink { e ->
        when (e) {
            is RuntimeEvent.Log -> println(e.line)
            is RuntimeEvent.TradeClosed -> println("CLOSED: ${e.trade}")
            is RuntimeEvent.Status -> println("STATUS: ${e.symbol} running=${e.running}")
        }
    }
    runBlocking {
        val runner = BotRunner(exchange, sink)
        val strategy = EmaAtrDemoStrategy(DemoParams())
        val spec = BotSpec(symbol = symbol, mode = RunMode.PAPER)
        runner.runPaper(spec, strategy, DemoParams())
        println("Press ENTER to stop...")
        readlnOrNull()
        runner.stop()
    }
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