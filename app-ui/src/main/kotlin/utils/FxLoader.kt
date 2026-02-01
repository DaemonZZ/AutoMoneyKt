package com.daemonz.utils

import com.daemonz.base.BaseController
import javafx.fxml.FXMLLoader
import org.koin.core.component.KoinComponent
import java.net.URL

object FxLoader : KoinComponent {

    data class Loaded(
        val controller: BaseController?,
        val root: Any
    )

    fun load(url: URL): Loaded {
        val loader = FXMLLoader(url)

        loader.setControllerFactory { clazz ->
            try {
                // ✅ Koin expects KClass
                getKoin().get(clazz.kotlin)
            } catch (_: Exception) {
                // fallback: no-arg constructor
                clazz.getDeclaredConstructor().newInstance()
            }
        }

        val root = loader.load<Any>()
        val controller = loader.getController<Any>() as? BaseController
        return Loaded(controller, root)
    }
}
