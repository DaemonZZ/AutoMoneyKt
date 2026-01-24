package com.daemonz.utils

import com.daemonz.runtime.Mode

object SystemConfig {
    var auth: SessionAuth = SessionAuth()
    var mode: Mode = Mode.SANDBOX
}