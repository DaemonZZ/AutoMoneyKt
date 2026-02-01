package com.daemonz.controller

import com.daemonz.base.BaseController
import com.daemonz.base.ViewLifecycle
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
            val loaded = FxLoader.load(url)
            ViewEntry(loaded.controller, loaded.root as Node)
        }

        // call lifecycle
        val current = contentHost.children.firstOrNull()
        val currentCtrl = current?.properties?.get("controller") as? ViewLifecycle
        currentCtrl?.onHide()

        // attach controller to node for later lookup
        entry.node.properties["controller"] = entry.controller

        contentHost.children.setAll(entry.node)

        (entry.controller as? ViewLifecycle)?.onShow()
    }
}
