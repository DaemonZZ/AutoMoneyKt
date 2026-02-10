package com.daemonz.di

import com.daemonz.adapters.binance.BinanceConfig
import com.daemonz.adapters.binance.BinanceFuturesAdapter
import com.daemonz.adapters.exchange.ExchangeAdapter
import com.daemonz.controller.BotsController
import com.daemonz.runtime.bot.BotFleetRegistry
import com.daemonz.runtime.bot.DefaultBotFleetRegistry
import com.daemonz.utils.SystemConfig
import com.daemonz.viewmodel.DeployBotWizardViewModel
import com.daemonz.viewmodel.MainViewModel
import org.koin.dsl.module

val appModule = module {
    // viewmodels
    factory { MainViewModel() }
    factory { DeployBotWizardViewModel(get()) }
    // runtime singletons
    single<BotFleetRegistry> { DefaultBotFleetRegistry() }
    // adapters
    factory<ExchangeAdapter> {
        BinanceFuturesAdapter(
            BinanceConfig(
                apiKey = SystemConfig.auth.apiKey,
                apiSecret = SystemConfig.auth.secret,
                baseUrl = SystemConfig.mode.baseUrl
            )
        )
    }
    //cotroller
    factory { BotsController(get()) }
}
