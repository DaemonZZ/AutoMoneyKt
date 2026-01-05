package com.daemonz.runtime.scanner

import com.daemonz.core.engine.BacktestResult

data class AnalyzeRow(
    val scan: ScanResult,
    val backtest: BacktestResult
)