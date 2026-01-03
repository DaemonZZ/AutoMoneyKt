package com.daemonz.controller

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import java.util.concurrent.ConcurrentHashMap

class MainController {

    lateinit var navBar: VBox
    @FXML
    lateinit var contentHost: StackPane

    @FXML
    lateinit var homeBtn: ToggleButton
    @FXML
    lateinit var analyzeBtn: ToggleButton
    @FXML
    lateinit var botseBtn: ToggleButton
    @FXML
    lateinit var logsBtn: ToggleButton

    @FXML
    lateinit var navGroup: ToggleGroup

    private val viewCache = ConcurrentHashMap<String, Parent>()

    @FXML
    fun initialize() {
        // 1) Wire navigation
        homeBtn.setOnAction { show("HomeView.fxml") }
        analyzeBtn.setOnAction { show("AnalyzeView.fxml") }
        botseBtn.setOnAction { show("BotsView.fxml") }
        logsBtn.setOnAction { show("LogsView.fxml") }

        // 2) Default view on startup
        homeBtn.isSelected = true
        show("HomeView.fxml")
    }

    private fun show(fxml: String) {
        val root = viewCache.computeIfAbsent(fxml) {
            FXMLLoader(javaClass.getResource("/$fxml")).load()
        }
        contentHost.children.setAll(root)
    }
}