package com.daemonz.risk

import kotlin.math.abs
import kotlin.math.max

object RiskEngine {

    /**
     * position sizing theo stop distance:
     * risk$ = equity * riskPct
     * qty = risk$ / |entry - sl|
     */
    fun calcQtyByStop(
        equity: Double,
        entry: Double,
        sl: Double,
        riskPct: Double
    ): Double {
        val riskUsd = equity * riskPct
        val stopDist = abs(entry - sl)
        if (stopDist <= 0.0) return 0.0
        return max(0.0, riskUsd / stopDist)
    }

    fun fees(entry: Double, exit: Double, qty: Double, feeRate: Double): Double {
        // fee on notional both sides (simplified)
        val notional = (abs(entry) + abs(exit)) * qty
        return notional * feeRate
    }

    fun applySlippage(price: Double, sideSign: Int, slippageRate: Double): Double {
        // sideSign: +1 buy worse (higher), -1 sell worse (lower)
        return price * (1.0 + sideSign * slippageRate)
    }
}