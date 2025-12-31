package com.daemonz.runtime

import com.daemonz.adapters.exchange.ConnectionStatus
import com.daemonz.adapters.exchange.ExchangeAdapter
import com.daemonz.adapters.exchange.FuturesBalance

class AccountService(private val exchange: ExchangeAdapter) {

    fun checkConnection(): ConnectionStatus = exchange.checkConnection()

    fun loadBalances(): List<FuturesBalance> = exchange.fetchFuturesBalances()

    fun getAccountInfo() = exchange.fetchFuturesAccountInfo()
}