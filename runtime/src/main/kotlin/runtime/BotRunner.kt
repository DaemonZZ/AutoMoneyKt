package com.daemonz.runtime

import com.daemonz.core.engine.BacktestEngine
import com.daemonz.adapters.exchange.ExchangeAdapter
import com.daemonz.core.risk.RiskConfig
import com.daemonz.core.strategy.Strategy
import kotlinx.coroutines.*
import kotlin.coroutines.cancellation.CancellationException

class BotRunner(
    private val exchange: ExchangeAdapter,
    private val eventSink: EventSink,
    parentScope: CoroutineScope = GlobalScope // sau sẽ inject
) {

    private val scope = parentScope + SupervisorJob()
    private var job: Job? = null

    fun stop() {
        job?.cancel()
    }

    fun <P> runPaper(
        spec: BotSpec,
        strategy: Strategy<P>,
        params: P,
        risk: RiskConfig = RiskConfig()
    ) {
        val symbol = spec.symbol
        if (!BotRegistry.tryLock(symbol)) {
            eventSink.emit(RuntimeEvent.Log("⚠️ Bot for $symbol already running"))
            return
        }

        job = scope.launch(Dispatchers.Default) {
            eventSink.emit(RuntimeEvent.Status(symbol, true))
            eventSink.emit(RuntimeEvent.Log("[Runtime] start PAPER bot: ${strategy.name()} on $symbol"))

            try {
                val engine = BacktestEngine(risk)
                var lastTradeCount = 0
                var step = 0

                while (isActive) {
                    val candles = exchange.fetchCandles(symbol, 600)
                    val res = engine.run(symbol, candles, strategy, params)

                    val newTrades = res.trades.drop(lastTradeCount)
                    newTrades.forEach {
                        eventSink.emit(RuntimeEvent.TradeClosed(it))
                        eventSink.emit(RuntimeEvent.Log("[Trade] ${it.side} pnl=${"%.4f".format(it.pnl)}"))
                    }
                    lastTradeCount = res.trades.size

                    if (step % 10 == 0) {
                        eventSink.emit(
                            RuntimeEvent.Log("[PAPER] equity=${"%.2f".format(res.endingEquity)} trades=${res.trades.size}")
                        )
                    }

                    step++
                    delay(300) // ⬅️ coroutine-friendly sleep
                }
            } catch (e: CancellationException) {
                eventSink.emit(RuntimeEvent.Log("[Runtime] bot cancelled"))
            } catch (t: Throwable) {
                eventSink.emit(RuntimeEvent.Log("❌ Bot crashed: ${t.message}"))
            } finally {
                BotRegistry.unlock(symbol)
                eventSink.emit(RuntimeEvent.Status(symbol, false))
                eventSink.emit(RuntimeEvent.Log("[Runtime] stop bot: $symbol"))
            }
        }
    }
}
