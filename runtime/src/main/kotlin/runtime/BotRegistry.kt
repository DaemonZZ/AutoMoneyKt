package com.daemonz.runtime

import java.util.concurrent.ConcurrentHashMap

object BotRegistry {
    private val running = ConcurrentHashMap.newKeySet<String>()

    fun tryLock(symbol: String): Boolean = running.add(symbol)
    fun unlock(symbol: String) {
        running.remove(symbol)
    }

    fun isRunning(symbol: String): Boolean = running.contains(symbol)
    fun runningSymbols(): Set<String> = running.toSet()
}