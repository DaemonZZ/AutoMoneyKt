package com.daemonz.runtime.status


enum class AppStatus(
    val label: String,
    val isBusy: Boolean = false
) {
    IDLE("Idle"),
    LOADING_SYMBOLS("Loading symbols…", true),
    ANALYZING("Analyzing market…", true),
    RUNNING_BOT("Bot running", true),
    CANCELLED("Cancelled"),
    ERROR("Error"),
    DONE("Done")
}