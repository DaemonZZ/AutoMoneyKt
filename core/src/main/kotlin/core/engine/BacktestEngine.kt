package com.daemonz.core.engine

import com.daemonz.core.market.Candle
import com.daemonz.core.risk.RiskConfig
import com.daemonz.core.signal.Signal
import com.daemonz.core.strategy.Strategy
import com.daemonz.core.strategy.StrategyContext
import com.daemonz.core.trade.Position
import com.daemonz.core.trade.Trade
import com.daemonz.core.trade.TradeLifecycle

data class BacktestResult(
    val trades: List<Trade>,
    val endingEquity: Double
)

class BacktestEngine(private val risk: RiskConfig = RiskConfig()) {

    fun <P> run(
        symbol: String,
        candles: List<Candle>,
        strategy: Strategy<P>,
        params: P
    ): BacktestResult {
        var equity = risk.startingEquity
        var position: Position = Position.Flat
        val trades = mutableListOf<Trade>()

        val startIdx = strategy.warmupBars(params).coerceAtLeast(0)
        for (i in startIdx until candles.size) {
            val bar = candles[i]
            val ctx = StrategyContext(
                symbol = symbol,
                position = position,
                equity = equity,
                barIndex = i
            )

            val signals: List<Signal> = strategy.onBar(ctx, bar, params)
            val step = TradeLifecycle.step(symbol, position, equity, bar, signals, risk)

            position = step.newPosition
            equity = step.newEquity
            step.closedTrade?.let(trades::add)
        }

        return BacktestResult(trades = trades, endingEquity = equity)
    }
}