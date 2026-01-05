package com.daemonz.runtime.scanner

import com.daemonz.core.trade.Trade
import kotlin.math.abs
import kotlin.math.max

object Stats {

    fun compute(trades: List<Trade>, startingEquity: Double, endingEquity: Double): ScanMetrics {
        val n = trades.size
        if (n == 0) {
            return ScanMetrics(
                trades = 0, winRate = 0.0, profitFactor = 0.0,
                expectancy = 0.0, maxDrawdownPct = 0.0, netPnl = endingEquity - startingEquity
            )
        }

        val wins = trades.count { it.pnl > 0 }
        val winRate = wins.toDouble() / n.toDouble()

        val grossProfit = trades.filter { it.pnl > 0 }.sumOf { it.pnl }
        val grossLoss = trades.filter { it.pnl < 0 }.sumOf { abs(it.pnl) }
        val profitFactor = if (grossLoss == 0.0) 99.0 else grossProfit / grossLoss

        val expectancy = trades.sumOf { it.pnl } / n.toDouble()
        val net = endingEquity - startingEquity

        // Max drawdown: xấp xỉ từ cumulative equity theo trades (basic)
        var peak = startingEquity
        var equity = startingEquity
        var maxDd = 0.0
        for (t in trades) {
            equity += t.pnl
            peak = max(peak, equity)
            val dd = if (peak == 0.0) 0.0 else (peak - equity) / peak
            maxDd = max(maxDd, dd)
        }

        return ScanMetrics(
            trades = n,
            winRate = winRate,
            profitFactor = profitFactor,
            expectancy = expectancy,
            maxDrawdownPct = maxDd * 100.0,
            netPnl = net
        )
    }
}