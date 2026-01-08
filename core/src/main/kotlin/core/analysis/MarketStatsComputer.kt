package com.daemonz.core.analysis

import com.daemonz.core.market.Candle

interface MarketStatsComputer {
    fun compute(candles: List<Candle>): MarketStats
}