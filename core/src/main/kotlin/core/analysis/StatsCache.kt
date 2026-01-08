package com.daemonz.core.analysis

/**
 * Key includes last candle open time to avoid stale stats when market advances.
 */
data class StatsKey(
    val symbol: String,
    val timeframe: String,
    val candleLimit: Int,
    val lastCandleTime: Long
)

interface StatsCache {
    fun get(key: StatsKey): MarketStats?
    fun put(key: StatsKey, stats: MarketStats)
    fun clear()
}

/**
 * Thread-safe LRU cache (size-limited).
 */
class LruStatsCache(
    private val maxSize: Int = 2000
) : StatsCache {

    private val map = object : LinkedHashMap<StatsKey, MarketStats>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<StatsKey, MarketStats>): Boolean {
            return size > maxSize
        }
    }

    @Synchronized
    override fun get(key: StatsKey): MarketStats? = map[key]
    @Synchronized
    override fun put(key: StatsKey, stats: MarketStats) {
        map[key] = stats
    }

    @Synchronized
    override fun clear() {
        map.clear()
    }
}
