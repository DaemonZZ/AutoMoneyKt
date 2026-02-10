package com.daemonz.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx

abstract class BaseViewModel {
    protected val viewModelScope = CoroutineScope(Dispatchers.JavaFx)
    protected val ioScope = CoroutineScope(Dispatchers.IO)
}