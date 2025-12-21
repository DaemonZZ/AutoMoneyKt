package com.daemonz.risk

data class RiskConfig(
    val startingEquity: Double = 1000.0,
    val riskPerTradePct: Double = 0.01,     // 1% equity risk
    val feeRate: Double = 0.0004,           // taker fee example
    val slippageRate: Double = 0.0          // paper/backtest default 0
)