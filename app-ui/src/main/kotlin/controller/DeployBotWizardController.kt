package com.daemonz.controller

import com.daemonz.core.strategy.StrategySpec
import com.daemonz.runtime.Mode
import com.daemonz.strategies.registry.StrategyRegistry
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.layout.VBox
import java.util.UUID


data class BotConfig(
    val botId: String = UUID.randomUUID().toString(),
    val name: String,
    val symbol: String,
    val mode: Mode,
    val exchange: String,
    val strategy: String,
    val preset: String,
    val loadDefaults: Boolean,
    val allowShort: Boolean,
    val riskProfile: String,
    val maxPositions: Int,
    val riskPerTradePct: Double,
    val dailyLossLimitPct: Double,
    val pauseOnError: Boolean,
    val pauseOnDisconnect: Boolean,
    val notes: String?
)

class DeployBotWizardController {

    // top
    @FXML
    lateinit var lblStep: Label
    @FXML
    lateinit var stepIdentity: Label
    @FXML
    lateinit var stepStrategy: Label
    @FXML
    lateinit var stepRisk: Label
    @FXML
    lateinit var lblError: Label

    // panes
    @FXML
    lateinit var paneStep1: VBox
    @FXML
    lateinit var paneStep2: VBox
    @FXML
    lateinit var paneStep3: VBox

    // step 1
    @FXML
    lateinit var txtBotName: TextField
    @FXML
    lateinit var cboSymbol: ComboBox<String>
    @FXML
    lateinit var tbLive: ToggleButton
    @FXML
    lateinit var tbSandBox: ToggleButton
    @FXML
    lateinit var cboExchange: ComboBox<String>
    @FXML
    lateinit var txtNotes: TextArea
    @FXML
    lateinit var liveWarningBox: VBox

    // step 2
    @FXML
    lateinit var cboStrategy: ComboBox<StrategySpec<*>>
    @FXML
    lateinit var cboPreset: ComboBox<String>
    @FXML
    lateinit var chkLoadDefaults: CheckBox
    @FXML
    lateinit var chkAllowShort: CheckBox
    @FXML
    lateinit var btnStrategyParams: Button
    @FXML
    lateinit var txtLogicSummary: TextArea

    // step 3
    @FXML
    lateinit var cboRiskProfile: ComboBox<String>
    @FXML
    lateinit var spMaxPos: Spinner<Int>
    @FXML
    lateinit var txtRiskPerTrade: TextField
    @FXML
    lateinit var txtDailyLoss: TextField
    @FXML
    lateinit var chkPauseOnError: CheckBox
    @FXML
    lateinit var chkPauseOnDisconnect: CheckBox
    @FXML
    lateinit var txtDeploySummary: TextArea

    // bottom
    @FXML
    lateinit var btnCancel: Button
    @FXML
    lateinit var btnBack: Button
    @FXML
    lateinit var btnNext: Button
    @FXML
    lateinit var btnCreate: Button

    var onCreate: ((BotConfig) -> Unit)? = null
    var onCancel: (() -> Unit)? = null

    private var step = 0
    private val modeGroup = ToggleGroup()

    @FXML
    fun initialize() {
        // defaults
        cboSymbol.items.setAll("BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT")
        cboSymbol.editor.text = "BTCUSDT"

        cboExchange.items.setAll("Binance Futures")
        cboExchange.selectionModel.selectFirst()

        cboStrategy.items.setAll(StrategyRegistry.all)
        cboStrategy.selectionModel.selectFirst()

        cboPreset.items.setAll("Default", "Conservative", "Aggressive")
        cboPreset.selectionModel.select("Conservative")

        cboRiskProfile.items.setAll("Conservative", "Normal", "Aggressive")
        cboRiskProfile.selectionModel.select("Normal")

        spMaxPos.valueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1)
        txtRiskPerTrade.text = "0.5"
        txtDailyLoss.text = "2.0"

        // mode group
        tbLive.toggleGroup = modeGroup
        tbSandBox.toggleGroup = modeGroup
        tbSandBox.isSelected = true

        modeGroup.selectedToggleProperty().addListener { _, _, _ ->
            val live = currentMode() == Mode.LIVE
            liveWarningBox.isVisible = live
            liveWarningBox.isManaged = live
            updateSummaries()
        }

        cboStrategy.valueProperty().addListener { _, _, _ -> updateSummaries() }
        cboPreset.valueProperty().addListener { _, _, _ -> updateSummaries() }
        chkAllowShort.selectedProperty().addListener { _, _, _ -> updateSummaries() }
        chkLoadDefaults.selectedProperty().addListener { _, _, _ -> updateSummaries() }
        spMaxPos.valueProperty().addListener { _, _, _ -> updateSummaries() }
        txtRiskPerTrade.textProperty().addListener { _, _, _ -> updateSummaries() }
        txtDailyLoss.textProperty().addListener { _, _, _ -> updateSummaries() }
        cboRiskProfile.valueProperty().addListener { _, _, _ -> updateSummaries() }

        btnStrategyParams.setOnAction {
            Alert(Alert.AlertType.INFORMATION).apply {
                title = "Strategy Params"
                headerText = null
                contentText = "Implement params dialog later. Currently using preset/defaults."
            }.showAndWait()
        }

        btnCancel.setOnAction { onCancel?.invoke() ?: close() }
        btnBack.setOnAction { stepBack() }
        btnNext.setOnAction { stepNext() }
        btnCreate.setOnAction { createBot() }

        render()
        updateSummaries()
    }

    private fun currentMode(): Mode = when (modeGroup.selectedToggle) {
        tbLive -> Mode.LIVE
        tbSandBox -> Mode.SANDBOX
        else -> Mode.SANDBOX
    }

    private fun stepBack() {
        clearErr()
        if (step == 0) return
        step--
        render()
    }

    private fun stepNext() {
        clearErr()
        val ok = when (step) {
            0 -> validateStep1()
            1 -> validateStep2()
            else -> true
        }
        if (!ok) return
        step++
        render()
    }

    private fun render() {
        paneStep1.isVisible = step == 0; paneStep1.isManaged = step == 0
        paneStep2.isVisible = step == 1; paneStep2.isManaged = step == 1
        paneStep3.isVisible = step == 2; paneStep3.isManaged = step == 2

        btnBack.isDisable = step == 0
        btnNext.isVisible = step < 2; btnNext.isManaged = step < 2
        btnCreate.isVisible = step == 2; btnCreate.isManaged = step == 2

        lblStep.text = "STEP ${step + 1}/3"

        // simple step indicator styles (inline)
        stepIdentity.style = stepStyle(step == 0, step > 0)
        stepStrategy.style = stepStyle(step == 1, step > 1)
        stepRisk.style = stepStyle(step == 2, false)

        updateSummaries()
    }

    private fun stepStyle(active: Boolean, done: Boolean): String {
        return when {
            active -> "-fx-background-color:#0ea5e9; -fx-text-fill:#031018; -fx-padding:6 10; -fx-background-radius:999; -fx-font-weight:700;"
            done -> "-fx-background-color:#10222b; -fx-text-fill:#d7e3ea; -fx-padding:6 10; -fx-background-radius:999;"
            else -> "-fx-border-color:#16303b; -fx-text-fill:#7f9aa8; -fx-padding:6 10; -fx-background-radius:999; -fx-border-radius:999; -fx-opacity:0.6;"
        }
    }

    private fun validateStep1(): Boolean {
        val name = txtBotName.text.trim()
        val symbol = cboSymbol.editor.text.trim()

        if (name.isBlank()) return err("Bot name is required.")
        if (!name.matches(Regex("^[A-Za-z0-9][A-Za-z0-9_-]{2,31}$")))
            return err("Bot name must be 3–32 chars (letters, digits, _ or -).")

        if (symbol.isBlank()) return err("Symbol is required (e.g. BTCUSDT).")
        if (!symbol.matches(Regex("^[A-Z0-9]{6,16}$")))
            return err("Symbol must be uppercase like BTCUSDT.")

        if (cboExchange.value.isNullOrBlank()) return err("Exchange is required.")
        return true
    }

    private fun validateStep2(): Boolean {
        if (cboStrategy.value.displayName.isBlank()) return err("Strategy is required.")
        if (cboPreset.value.isNullOrBlank()) return err("Preset is required.")
        return true
    }

    private fun validateStep3(): Boolean {
        val rp = txtRiskPerTrade.text.trim().toDoubleOrNull() ?: return err("Risk per trade must be a number.")
        val dl = txtDailyLoss.text.trim().toDoubleOrNull() ?: return err("Daily loss limit must be a number.")
        if (rp <= 0.0 || rp > 10.0) return err("Risk per trade should be in (0, 10].")
        if (dl <= 0.0 || dl > 50.0) return err("Daily loss limit should be in (0, 50].")
        return true
    }

    private fun createBot() {
        clearErr()
        if (!validateStep1() || !validateStep2() || !validateStep3()) return

        if (currentMode() == Mode.LIVE) {
            val ok = Alert(Alert.AlertType.CONFIRMATION).apply {
                title = "Confirm LIVE deployment"
                headerText = null
                contentText = "You are about to deploy a LIVE bot.\nReal orders will be placed.\n\nContinue?"
            }.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK
            if (!ok) return
        }

        val cfg = BotConfig(
            name = txtBotName.text.trim(),
            symbol = cboSymbol.editor.text.trim(),
            mode = currentMode(),
            exchange = cboExchange.value.trim(),
            strategy = cboStrategy.value.displayName,
            preset = cboPreset.value.trim(),
            loadDefaults = chkLoadDefaults.isSelected,
            allowShort = chkAllowShort.isSelected,
            riskProfile = (cboRiskProfile.value ?: "Normal").trim(),
            maxPositions = spMaxPos.value,
            riskPerTradePct = txtRiskPerTrade.text.trim().toDouble(),
            dailyLossLimitPct = txtDailyLoss.text.trim().toDouble(),
            pauseOnError = chkPauseOnError.isSelected,
            pauseOnDisconnect = chkPauseOnDisconnect.isSelected,
            notes = txtNotes.text.trim().takeIf { it.isNotBlank() }
        )

        onCreate?.invoke(cfg)
        close()
    }

    private fun updateSummaries() {
        val s = cboStrategy.value ?: ""
        val p = cboPreset.value ?: ""
        val allowShort = if (chkAllowShort.isSelected) "YES" else "NO"
        val defaults = if (chkLoadDefaults.isSelected) "YES" else "NO"

        txtLogicSummary.text =
            "Strategy: $s\nPreset: $p\nLoad defaults: $defaults\nShorting enabled: $allowShort\n\n" +
                    when (s) {
                        "EMA Pullback" -> "Looks for pullbacks to EMA in confirmed trends; filtered by volatility/regime gates."
                        "ATR Donchian" -> "Looks for Donchian breakouts; filtered by ATR volatility and liquidity gates."
                        else -> ""
                    }

        val name = txtBotName.text.trim().ifBlank { "(unnamed)" }
        val symbol = cboSymbol.editor.text.trim().ifBlank { "(symbol)" }
        val mode = currentMode().name
        val riskProfile = cboRiskProfile.value ?: "Normal"
        val maxPos = spMaxPos.value
        val r = txtRiskPerTrade.text.trim().ifBlank { "?" }
        val d = txtDailyLoss.text.trim().ifBlank { "?" }

        txtDeploySummary.text =
            "Deploy $name on $symbol in $mode mode using $s ($p).\n" +
                    "Risk profile: $riskProfile. Risk $r%/trade, max $maxPos positions.\n" +
                    "Trading halts if daily loss exceeds $d%."
    }

    private fun err(msg: String): Boolean {
        lblError.text = msg; return false
    }

    private fun clearErr() {
        lblError.text = ""
    }

    private fun close() {
        lblStep.scene?.window?.hide()
    }
}
