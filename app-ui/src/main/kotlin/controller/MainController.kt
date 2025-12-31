package com.daemonz.controller

import com.daemonz.base.BaseController
import com.daemonz.controller.dialog.AuthenticationDialogController
import com.daemonz.utils.Mode
import com.daemonz.utils.SystemConfig
import com.daemonz.viewmodel.MainViewModel
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.util.StringConverter
import org.koin.java.KoinJavaComponent.inject
import java.util.Map


class MainController : BaseController() {

    lateinit var modeChoice: ChoiceBox<Mode>
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
        modeChoice.items = FXCollections.observableArrayList(Mode.entries)
        modeChoice.converter = object : StringConverter<Mode>() {
            override fun toString(p0: Mode?): String? = p0?.label

            override fun fromString(p0: String?): Mode? =
                Mode.entries.find { it.label == p0 }

        }
        modeChoice.selectionModel.select(SystemConfig.mode)
        modeChoice.valueProperty().addListener { _, _, newValue ->
            println("Mode selected: $newValue")
            SystemConfig.mode = newValue
        }
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
