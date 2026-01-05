package com.daemonz.core.engine

import com.daemonz.core.market.Candle
import com.daemonz.core.risk.RiskConfig
import com.daemonz.core.signal.Signal
import com.daemonz.core.strategy.Strategy
import com.daemonz.core.trade.Position
import com.daemonz.core.trade.Trade
import com.daemonz.core.trade.TradeLifecycle

data class BacktestResult(
    val trades: List<Trade>,
    val endingEquity: Double,
    val equityCurve: List<EquityPoint>
)

class BacktestEngine(private val risk: RiskConfig = RiskConfig()) {

    fun <P> run(
        symbol: String,
        candles: List<Candle>,
        strategy: Strategy<P>,
        params: P
    ): BacktestResult {

        if (candles.isEmpty()) {
            return BacktestResult(
                trades = emptyList(),
                endingEquity = risk.startingEquity,
                equityCurve = emptyList()
            )
        }

        var equity = risk.startingEquity
        var position: Position = Position.Flat
        val trades = mutableListOf<Trade>()
        val curve = ArrayList<EquityPoint>(candles.size)

        // IMPORTANT: always feed candles from the beginning so indicators can warm up.
        for (i in candles.indices) {
            val bar = candles[i]

            // 1) Update strategy internal state (EMA/ATR/lastClose...)
            strategy.onCandle(bar)

            // 2) Get at most one signal per bar from strategy (nullable)
            val signal: Signal? = when (position) {
                Position.Flat -> strategy.shouldEnter()
                is Position.LongPos, is Position.ShortPos -> strategy.shouldExit(position)
            }

            // 3) Convert to list for lifecycle engine
            val signals: List<Signal> = signal?.let { listOf(it) } ?: emptyList()

            val step = TradeLifecycle.step(
                symbol = symbol,
                position = position,
                equity = equity,
                bar = bar,
                signals = signals,
                risk = risk
            )

            position = step.newPosition
            equity = step.newEquity
            step.closedTrade?.let(trades::add)

            // 4) Track equity curve at each bar close
            curve.add(EquityPoint(t = bar.t, equity = equity))
        }

        return BacktestResult(
            trades = trades,
            endingEquity = equity,
            equityCurve = curve
        )
    }

}
