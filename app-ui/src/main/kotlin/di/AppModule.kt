package com.daemonz.di

import com.daemonz.controller.BotsController
import com.daemonz.runtime.bot.BotFleetRegistry
import com.daemonz.runtime.bot.DefaultBotFleetRegistry
import com.daemonz.viewmodel.MainViewModel
import org.koin.dsl.module

val appModule = module {
    // viewmodels
    factory { MainViewModel() }
    // runtime singletons
    single<BotFleetRegistry> { DefaultBotFleetRegistry() }
    //cotroller
    factory { BotsController(get()) }
}