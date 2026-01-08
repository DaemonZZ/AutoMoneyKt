package com.daemonz.core.analysis

import com.daemonz.core.market.Candle
import kotlin.math.abs

class SimpleMarketStatsComputer(
    private val atrPeriod: Int = 14,
    private val emaPeriod: Int = 50,
    private val chopLookback: Int = 50
) : MarketStatsComputer {

    override fun compute(candles: List<Candle>): MarketStats {
        val minBars = maxOf(atrPeriod, emaPeriod, chopLookback) + 2
        require(candles.size >= minBars) {
            "Not enough candles: need >= $minBars, got ${candles.size}"
        }

        val closes = candles.map { it.close }
        val highs = candles.map { it.high }
        val lows = candles.map { it.low }

        val atr = atr(highs, lows, closes, atrPeriod)
        val lastClose = closes.last().coerceAtLeast(1e-9)
        val atrPct = (atr / lastClose) * 100.0

        val emaSeries = ema(closes, emaPeriod)
        val slope = emaSeries.last() - emaSeries[emaSeries.size - 2]

        val trendStrength = (abs(slope) / atr.coerceAtLeast(1e-9))
            .coerceIn(0.0, 1.0)

        // Chop proxy:
        // directionalRatio = range / sum(TR) over a lookback
        // smaller => choppier (more back-and-forth), thus chopScore higher.
        val slice = candles.takeLast(chopLookback)
        val range = (slice.maxOf { it.high } - slice.minOf { it.low }).coerceAtLeast(1e-9)

        val sliceHighs = slice.map { it.high }
        val sliceLows = slice.map { it.low }
        val sliceCloses = slice.map { it.close }

        val trSum = trueRanges(sliceHighs, sliceLows, sliceCloses)
            .sum()
            .coerceAtLeast(1e-9)

        val directionalRatio = (range / trSum).coerceIn(0.0, 1.0)
        val chopScore = (1.0 - directionalRatio).coerceIn(0.0, 1.0)

        return MarketStats(
            atrPct = atrPct,
            trendStrength = trendStrength,
            chopScore = chopScore,
            liquidityScore = 1.0
        )
    }

    private fun ema(values: List<Double>, period: Int): List<Double> {
        val k = 2.0 / (period + 1.0)
        val out = ArrayList<Double>(values.size)
        var prev = values.first()
        out.add(prev)
        for (i in 1 until values.size) {
            val v = values[i]
            val cur = (v - prev) * k + prev
            out.add(cur)
            prev = cur
        }
        return out
    }

    private fun atr(highs: List<Double>, lows: List<Double>, closes: List<Double>, period: Int): Double {
        val trs = trueRanges(highs, lows, closes)
        return trs.takeLast(period).average()
    }

    private fun trueRanges(highs: List<Double>, lows: List<Double>, closes: List<Double>): List<Double> {
        if (highs.size < 2) return emptyList()
        val trs = ArrayList<Double>(highs.size - 1)
        for (i in 1 until highs.size) {
            val h = highs[i]
            val l = lows[i]
            val prevClose = closes[i - 1]
            val tr = maxOf(
                h - l,
                abs(h - prevClose),
                abs(l - prevClose)
            )
            trs.add(tr)
        }
        return trs
    }
}