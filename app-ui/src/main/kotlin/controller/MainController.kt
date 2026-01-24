package com.daemonz.controller

import com.daemonz.base.BaseController
import com.daemonz.utils.FxLoader
import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import java.util.concurrent.ConcurrentHashMap

class MainController {

    @FXML
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

    private data class ViewEntry(val controller: BaseController?, val node: Node)

    private val viewCache = ConcurrentHashMap<String, ViewEntry>()

    @FXML
    fun initialize() {
        // Ensure toggle group (if not set in FXML)
        homeBtn.toggleGroup = navGroup
        analyzeBtn.toggleGroup = navGroup
        botseBtn.toggleGroup = navGroup
        logsBtn.toggleGroup = navGroup

        homeBtn.setOnAction { show("/HomeView.fxml") }
        analyzeBtn.setOnAction { show("/AnalyzeView.fxml") }
        botseBtn.setOnAction { show("/BotsView.fxml") }
        logsBtn.setOnAction { show("/LogsView.fxml") }

        homeBtn.isSelected = true
        show("/HomeView.fxml")
    }

    private fun show(fxmlPath: String) {
        val entry = viewCache.computeIfAbsent(fxmlPath) { path ->
            val url = requireNotNull(javaClass.getResource(path)) { "FXML not found: $path" }
            val (controller, root) = FxLoader.load<BaseController>(url)
            ViewEntry(controller, root as Node)
        }
        contentHost.children.setAll(entry.node)
    }
}
