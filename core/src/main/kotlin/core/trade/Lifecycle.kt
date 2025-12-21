package com.daemonz.core.trade

import com.daemonz.core.market.Candle
import com.daemonz.core.risk.RiskConfig
import com.daemonz.core.risk.RiskEngine
import com.daemonz.core.signal.Signal

data class LifecycleResult(
    val newPosition: Position,
    val newEquity: Double,
    val closedTrade: Trade? = null
)

object TradeLifecycle {

    fun step(
        symbol: String,
        position: Position,
        equity: Double,
        bar: Candle,
        signals: List<Signal>,
        risk: RiskConfig
    ): LifecycleResult {
        var pos = position
        var eq = equity
        var closed: Trade? = null

        for (s in signals) {
            when (s) {
                is Signal.EnterLong -> if (pos is Position.Flat) {
                    val entry = RiskEngine.applySlippage(bar.close, +1, risk.slippageRate)
                    val qty = RiskEngine.calcQtyByStop(eq, entry, s.sl, risk.riskPerTradePct)
                    if (qty > 0.0) {
                        pos = Position.LongPos(entry, qty, s.sl, s.tp, bar.t)
                    }
                }

                is Signal.EnterShort -> if (pos is Position.Flat) {
                    val entry = RiskEngine.applySlippage(bar.close, -1, risk.slippageRate)
                    val qty = RiskEngine.calcQtyByStop(eq, entry, s.sl, risk.riskPerTradePct)
                    if (qty > 0.0) {
                        pos = Position.ShortPos(entry, qty, s.sl, s.tp, bar.t)
                    }
                }

                is Signal.UpdateSLTP -> {
                    pos = when (pos) {
                        is Position.LongPos -> pos.copy(
                            sl = s.sl ?: pos.sl,
                            tp = s.tp ?: pos.tp
                        )
                        is Position.ShortPos -> pos.copy(
                            sl = s.sl ?: pos.sl,
                            tp = s.tp ?: pos.tp
                        )
                        else -> pos
                    }
                }

                Signal.Exit -> {
                    if (pos !is Position.Flat) {
                        val (trade, newEq) = closePosition(symbol, pos, eq, bar, risk)
                        closed = trade
                        eq = newEq
                        pos = Position.Flat
                    }
                }

                Signal.NoOp -> Unit
            }
        }

        // Auto close if TP/SL hit (using bar close for now; later can use high/low intrabar)
        if (pos !is Position.Flat) {
            val hit = when (pos) {
                is Position.LongPos -> (bar.close >= pos.tp) || (bar.close <= pos.sl)
                is Position.ShortPos -> (bar.close <= pos.tp) || (bar.close >= pos.sl)
                else -> false
            }
            if (hit) {
                val (trade, newEq) = closePosition(symbol, pos, eq, bar, risk)
                closed = trade
                eq = newEq
                pos = Position.Flat
            }
        }

        return LifecycleResult(newPosition = pos, newEquity = eq, closedTrade = closed)
    }

    private fun closePosition(
        symbol: String,
        pos: Position,
        equity: Double,
        bar: Candle,
        risk: RiskConfig
    ): Pair<Trade, Double> {
        return when (pos) {
            is Position.LongPos -> {
                val exit = RiskEngine.applySlippage(bar.close, -1, risk.slippageRate)
                val fees = RiskEngine.fees(pos.entryPrice, exit, pos.qty, risk.feeRate)
                val pnl = (exit - pos.entryPrice) * pos.qty - fees
                val trade = Trade(symbol, Side.LONG, pos.qty, pos.entryPrice, exit, pos.entryTime, bar.t, pnl, fees)
                trade to (equity + pnl)
            }
            is Position.ShortPos -> {
                val exit = RiskEngine.applySlippage(bar.close, +1, risk.slippageRate)
                val fees = RiskEngine.fees(pos.entryPrice, exit, pos.qty, risk.feeRate)
                val pnl = (pos.entryPrice - exit) * pos.qty - fees
                val trade = Trade(symbol, Side.SHORT, pos.qty, pos.entryPrice, exit, pos.entryTime, bar.t, pnl, fees)
                trade to (equity + pnl)
            }
            Position.Flat -> error("closePosition called on Flat")
        }
    }
}