package com.daemonz.controller

import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.ProgressBar
import javafx.scene.control.RadioButton
import javafx.scene.control.Slider
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.control.ToggleButton

class MarketAnalyzerController {
    lateinit var excludeLeveragedCheck: CheckBox
    lateinit var excludeStablecoinsCheck: CheckBox
    lateinit var autoPickBtn: Button
    lateinit var autoPickGainersRadio: RadioButton
    lateinit var autoPickRandomRadio: RadioButton
    lateinit var autoPickTopVolumeRadio: RadioButton
    lateinit var hideLowSampleCheck: CheckBox
    lateinit var minVolatilityField: TextField
    lateinit var minScoreValueLabel: Label
    lateinit var minScoreSlider: Slider
    lateinit var minTradesValueLabel: Label
    lateinit var minTradesSlider: Slider
    lateinit var progressTextLabel: Label
    lateinit var statusLabel: Label
    lateinit var progressBar: ProgressBar
    lateinit var exportBtn: Button
    lateinit var cancelBtn: Button
    lateinit var analyzeBtn: Button
    lateinit var colMaxDd: TableColumn<Any, Any>
    lateinit var colPf: TableColumn<Any, Any>
    lateinit var colWinRate: TableColumn<Any, Any>
    lateinit var colTrades: TableColumn<Any, Any>
    lateinit var colScore: TableColumn<Any, Any>
    lateinit var colVerdict: TableColumn<Any, Any>
    lateinit var colSymbol: TableColumn<Any, Any>
    lateinit var resultsTable: TableView<Any>
    lateinit var resultsCountLabel: Label
    lateinit var clearSelectionBtn: Button
    lateinit var selectedCountLabel: Label
    lateinit var symbolSearchField: TextField
    lateinit var symbolListView: ListView<Any>
    lateinit var autoPickToggle: ToggleButton
    lateinit var manualToggle: ToggleButton
    lateinit var slippageCheck: CheckBox
    lateinit var feesCheck: CheckBox
    lateinit var equityField: TextField
    lateinit var riskField: TextField
    lateinit var modeCombo: ComboBox<Any>
    lateinit var historyCombo: ComboBox<Any>
    lateinit var intervalCombo: ComboBox<Any>
    lateinit var strategyCombo: ComboBox<Any>
}