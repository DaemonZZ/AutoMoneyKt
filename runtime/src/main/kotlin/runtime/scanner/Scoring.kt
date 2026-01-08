package com.daemonz.runtime.scanner

import kotlin.math.max
import kotlin.math.min

object Scoring {

    fun score(m: ScanMetrics): Pair<Int, Pair<Verdict, List<String>>> {
        val reasons = mutableListOf<String>()
        var s = 50.0

        if (m.trades < 10) {
            s -= 20
            reasons += "Ít giao dịch (${m.trades}) → độ tin cậy thấp"
        } else if (m.trades > 50) {
            s += 5
            reasons += "Số mẫu ổn (${m.trades} trades)"
        }

        if (m.expectancy > 0) {
            s += 15
            reasons += "Expectancy dương (${fmt(m.expectancy)})"
        } else {
            s -= 15
            reasons += "Expectancy âm (${fmt(m.expectancy)})"
        }

        if (m.profitFactor >= 1.5) {
            s += 15
            reasons += "Profit Factor tốt (${fmt(m.profitFactor)})"
        } else if (m.profitFactor < 1.0) {
            s -= 20
            reasons += "Profit Factor < 1 (${fmt(m.profitFactor)})"
        } else {
            reasons += "Profit Factor trung bình (${fmt(m.profitFactor)})"
        }

        if (m.maxDrawdownPct <= 10.0) {
            s += 10
            reasons += "Drawdown thấp (${fmt(m.maxDrawdownPct)}%)"
        } else if (m.maxDrawdownPct > 25.0) {
            s -= 25
            reasons += "Drawdown cao (${fmt(m.maxDrawdownPct)}%)"
        } else {
            s -= 5
            reasons += "Drawdown trung bình (${fmt(m.maxDrawdownPct)}%)"
        }

        if (m.netPnl > 0) reasons += "Net PnL +${fmt(m.netPnl)}"
        else reasons += "Net PnL ${fmt(m.netPnl)}"

        val score = min(100, max(0, s.toInt()))

        val verdict = when {
            score >= 70 && m.trades >= 10 -> Verdict.TRADE
            score >= 50 -> Verdict.WATCH
            else -> Verdict.SKIP
        }

        return score to (verdict to reasons.take(4))
    }

    private fun fmt(x: Double) = String.format("%.4f", x)
}