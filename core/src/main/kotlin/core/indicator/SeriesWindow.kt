package com.daemonz.core.indicator

/**
 * Rolling window fixed-size for Double values.
 * - push O(1)
 * - getLast / getAtFromEnd O(1)
 * Not thread-safe (core assumes single-threaded engine step).
 */
class SeriesWindow(private val capacity: Int) {
    init { require(capacity > 0) }

    private val buf = DoubleArray(capacity)
    private var size = 0
    private var head = 0 // next write index

    fun clear() {
        size = 0
        head = 0
    }

    fun push(v: Double) {
        buf[head] = v
        head = (head + 1) % capacity
        if (size < capacity) size++
    }

    fun size(): Int = size
    fun capacity(): Int = capacity
    fun isFull(): Boolean = size == capacity
    fun isEmpty(): Boolean = size == 0

    /** last pushed value */
    fun last(): Double {
        check(size > 0) { "SeriesWindow is empty" }
        val idx = (head - 1 + capacity) % capacity
        return buf[idx]
    }

    /**
     * Get element k from the end: k=0 => last(), k=1 => previous bar...
     */
    fun fromEnd(k: Int): Double {
        require(k >= 0) { "k must be >= 0" }
        check(k < size) { "k=$k out of range, size=$size" }
        val idx = (head - 1 - k + capacity * 10) % capacity
        return buf[idx]
    }

    /** Copy current values oldest->newest (debug/report only). */
    fun toList(): List<Double> {
        val out = ArrayList<Double>(size)
        val start = if (size < capacity) 0 else head
        for (i in 0 until size) {
            out += buf[(start + i) % capacity]
        }
        return out
    }
}