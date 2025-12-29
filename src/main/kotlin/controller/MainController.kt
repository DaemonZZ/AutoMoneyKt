package com.daemonz.controller

import com.daemonz.base.BaseController
import com.daemonz.viewmodel.MainViewModel
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import org.koin.java.KoinJavaComponent.inject
import java.util.Map


class MainController : BaseController() {

    lateinit var lblErrors: Label
    lateinit var lblRunner: Label
    lateinit var lblLastCandle: Label
    lateinit var lblFeed: Label
    lateinit var statusBar: HBox
    lateinit var contentHost: StackPane
    lateinit var navBar: VBox
    private val viewModel: MainViewModel by inject(MainViewModel::class.java)

    @FXML
    private lateinit var homeBtn: ToggleButton
    @FXML
    private lateinit var analyzeBtn: ToggleButton
    @FXML
    private lateinit var botseBtn: ToggleButton
    @FXML
    private lateinit var logsBtn: ToggleButton
    private val navGroup = ToggleGroup()
    private val viewMap = mutableMapOf<ToggleButton, String>()


    @FXML
    fun initialize() {
        viewMap[homeBtn] = "/HomeView.fxml"
        viewMap[analyzeBtn] = "/AnalyzeView.fxml"
        viewMap[botseBtn] = "/BotsView.fxml"
        viewMap[logsBtn] = "/LogsView.fxml"
        homeBtn.setOnAction {
            println("Home clicked")
        }
        // Nav toggle group
        listOf(homeBtn, analyzeBtn, botseBtn, logsBtn).forEach {
            it.toggleGroup = navGroup
        }

        navGroup.selectedToggleProperty().addListener { _, _, new ->
            if (new != null) {
                val btn = new as ToggleButton
                viewMap[btn]?.let { loadView(it) }
            }
        }

        homeBtn.isSelected = true
    }

    @FXML
    fun onHome() {
        println("onHome")
    }

    private fun loadView(fxml: String) {
        try {
            val node: Node = FXMLLoader.load(javaClass.getResource(fxml))
            contentHost.children.setAll(node)
        } catch (e: Exception) {
            contentHost.children.setAll(
                Label("Failed to load view:\n$fxml\n${e.message}")
            )
        }
    }
}
