package com.daemonz.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx

abstract class BaseController : ViewLifecycle {
    protected val uiScope = CoroutineScope(Dispatchers.JavaFx)
}