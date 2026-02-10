package com.daemonz.viewmodel

import com.daemonz.adapters.exchange.ExchangeAdapter
import com.daemonz.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DeployBotWizardViewModel(
    private val exchange: ExchangeAdapter
) : BaseViewModel() {
    private val _listSymbol: MutableStateFlow<List<String>> = MutableStateFlow(listOf())
    val symbols: StateFlow<List<String>> = _listSymbol

    fun getListSymbols() = ioScope.launch {
        println("getting list symbols")
        val list = exchange.listTradableSymbols()
        viewModelScope.launch {
            println("emitting list symbols: ${list}")
            _listSymbol.emit(list)
        }
    }

}
