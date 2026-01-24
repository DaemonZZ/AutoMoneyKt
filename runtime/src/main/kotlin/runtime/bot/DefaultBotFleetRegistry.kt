package com.daemonz.runtime.bot

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DefaultBotFleetRegistry : BotFleetRegistry {

    private val _runners = MutableStateFlow<List<BotRunner>>(emptyList())
    override val runners: StateFlow<List<BotRunner>> = _runners

    override fun get(botId: String): BotRunner? =
        _runners.value.firstOrNull { it.botId == botId }

    /* ===== registry operations ===== */

    fun setAll(list: List<BotRunner>) {
        _runners.value = list
    }

    fun add(runner: BotRunner) {
        _runners.value = _runners.value + runner
    }

    fun remove(botId: String) {
        _runners.value = _runners.value.filterNot { it.botId == botId }
    }

    fun clear() {
        _runners.value = emptyList()
    }
}
