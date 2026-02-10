package com.daemonz.models

import com.daemonz.runtime.Mode
import java.util.UUID

data class BotConfig(
    val botId: String = UUID.randomUUID().toString(),
    val name: String,
    val symbol: String,
    val mode: Mode,
    val exchange: String,
    val strategy: String,
    val preset: String,
    val loadDefaults: Boolean,
    val allowShort: Boolean,
    val riskProfile: String,
    val maxPositions: Int,
    val riskPerTradePct: Double,
    val dailyLossLimitPct: Double,
    val pauseOnError: Boolean,
    val pauseOnDisconnect: Boolean,
    val notes: String?
)