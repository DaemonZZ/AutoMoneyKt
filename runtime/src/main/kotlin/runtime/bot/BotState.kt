package com.daemonz.runtime.bot

import com.daemonz.runtime.Mode
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import java.time.Instant

enum class BotStatus { IDLE, ANALYZING, RUNNING, PAUSED, STOPPED, ERROR }

data class BotMetrics(
    val totalPnlUsd: Double = 0.0,
    val pnlTodayPct: Double = 0.0,
    val trades: Int = 0,
    val winRate: Double = 0.0,        // 0..1
    val exposureText: String = "0.00",
    val positions: Int = 0,
    val healthScore: Int = 0,         // 0..100
    val latencyMs: Int = 0,
    val lastActionAt: Instant? = null,
    val uptimeSec: Long = 0
)

data class BotState(
    val botId: String,
    val name: String,
    val symbol: String,
    val mode: Mode,
    val status: BotStatus,
    val metrics: BotMetrics = BotMetrics()
)

enum class LogLevel { SYSTEM, NETWORK, ANALYSIS, SIGNAL, ORDER, SUCCESS, ERROR, INFO, HEARTBEAT }

data class LogEvent(
    val botId: String,
    val ts: Instant = Instant.now(),
    val level: LogLevel,
    val message: String
)

interface BotRunner {
    val botId: String
    val state: StateFlow<BotState>
    val logs: SharedFlow<LogEvent>

    suspend fun start()
    suspend fun pause()
    suspend fun stop()
}
