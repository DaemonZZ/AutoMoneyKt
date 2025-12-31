package com.daemonz.di

import com.daemonz.viewmodel.MainViewModel
import org.koin.dsl.module

val appModule = module {
// viewmodels
    factory { MainViewModel() }
}