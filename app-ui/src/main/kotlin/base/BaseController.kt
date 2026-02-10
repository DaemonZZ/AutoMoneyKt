package com.daemonz.base

import javafx.fxml.FXML
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import org.koin.core.component.KoinComponent

abstract class BaseController : KoinComponent, ViewLifecycle {
    protected val uiScope = CoroutineScope(Dispatchers.JavaFx)


    @FXML
    open fun initialize() {
        setupObserver()
        initUi()
        fetchData()
    }

    abstract fun fetchData()

    abstract fun initUi()
    abstract fun setupObserver()
}