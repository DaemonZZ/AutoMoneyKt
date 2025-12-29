package com.daemonz.nav

sealed class Route {
    data object Auth : Route()
    data object Strategy : Route()
    data object Scanner : Route()
    data class Runner(val symbol: String) : Route()
    data object Report : Route()
}