package com.daemonz

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class App : Application() {
    override fun start(stage: Stage) {
        val root = FXMLLoader(
            javaClass.getResource("/main.fxml")
        ).load<javafx.scene.Parent>()

        stage.scene = Scene(root)
        stage.title = "JavaFX + Kotlin"
        stage.show()
    }
}
