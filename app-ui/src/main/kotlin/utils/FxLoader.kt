package com.daemonz.utils

import javafx.fxml.FXMLLoader
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.net.URL
import javax.swing.UIManager.get

object FxLoader : KoinComponent {

    fun <T> load(url: URL): Pair<T, Any> {
        val loader = FXMLLoader(url)

        loader.setControllerFactory { clazz ->
            // Nếu class được Koin quản lý -> Koin tạo
            // Nếu không -> fallback no-arg constructor
            try {
                get(clazz.kotlin)
            } catch (_: Exception) {
                clazz.getDeclaredConstructor().newInstance()
            }
        }

        val root = loader.load<Any>()
        @Suppress("UNCHECKED_CAST")
        return loader.getController<T>() to root
    }
}
