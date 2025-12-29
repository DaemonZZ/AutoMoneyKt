package com.daemonz

import com.daemonz.di.appModule
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import org.koin.core.context.GlobalContext.startKoin
import org.koin.java.KoinJavaComponent.getKoin

class App : Application() {
    override fun start(stage: Stage) {
        startKoin { modules(appModule) }
        val loader = FXMLLoader(
            javaClass.getResource("/main.fxml")
        )
        val root = loader.load<javafx.scene.Parent>()
        loader.setControllerFactory { clazz ->
            // Koin will construct controller and inject its constructor params
            getKoin().get(clazz.kotlin)
        }
        val scene = Scene(root, 1100.0, 720.0)
        val css = javaClass.getResource("/com/automoney/ui/styles/app.css");
        if (css != null) scene.stylesheets.add(css.toExternalForm());
        stage.scene = scene
        stage.title = "Auto Money"
        stage.show()
    }
}
