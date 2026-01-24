package com.daemonz.runtime.bot

import kotlinx.coroutines.flow.StateFlow

interface BotFleetRegistry {
    /** List bot runners currently registered in app */
    val runners: StateFlow<List<BotRunner>>

    fun get(botId: String): BotRunner?
}
