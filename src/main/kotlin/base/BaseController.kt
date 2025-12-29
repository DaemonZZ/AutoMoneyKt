package com.daemonz.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx

abstract class BaseController {
    protected val uiScope = CoroutineScope(Dispatchers.JavaFx)
}