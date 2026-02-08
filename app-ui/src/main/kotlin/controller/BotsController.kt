package com.daemonz.controller

import com.daemonz.base.BaseController
import com.daemonz.models.BotRow
import com.daemonz.runtime.Mode
import com.daemonz.runtime.bot.*
import javafx.collections.FXCollections
import javafx.collections.transformation.FilteredList
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.stage.Modality
import javafx.stage.Stage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.javafx.JavaFx
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

class BotsController(
    // inject bằng DI hoặc ServiceLocator
    private val registry: BotFleetRegistry
) : BaseController() {

    lateinit var btnDeployBot: Button

    // ====== FXML ids (the same as your screen) ======
    @FXML
    private lateinit var searchField: TextField

    @FXML
    private lateinit var modeFilterCombo: ComboBox<String>

    @FXML
    private lateinit var statusFilterCombo: ComboBox<String>

    @FXML
    private lateinit var botsTable: TableView<BotRow>

    @FXML
    private lateinit var colBotName: TableColumn<BotRow, String>

    @FXML
    private lateinit var colSymbol: TableColumn<BotRow, String>

    @FXML
    private lateinit var colStatus: TableColumn<BotRow, String>

    @FXML
    private lateinit var colMode: TableColumn<BotRow, String>

    @FXML
    private lateinit var lblFleetCount: Label

    @FXML
    private lateinit var lblActiveBots: Label

    // detail
    @FXML
    private lateinit var lblBotTitle: Label

    @FXML
    private lateinit var lblBotSubtitle: Label

    @FXML
    private lateinit var lblBotStatusBadge: Label

    @FXML
    private lateinit var lblTotalPnl: Label

    @FXML
    private lateinit var lblPnlHint: Label

    @FXML
    private lateinit var lblWinRate: Label

    @FXML
    private lateinit var lblTrades: Label

    @FXML
    private lateinit var lblExposure: Label

    @FXML
    private lateinit var lblPositions: Label

    @FXML
    private lateinit var lblHealth: Label

    @FXML
    private lateinit var lblLatency: Label

    @FXML
    private lateinit var chkAutoScroll: CheckBox

    @FXML
    private lateinit var logArea: TextArea

    @FXML
    private lateinit var btnStart: Button

    @FXML
    private lateinit var btnPause: Button

    @FXML
    private lateinit var btnStop: Button

    @FXML
    private lateinit var btnClearLog: Button


    private val rows = FXCollections.observableArrayList<BotRow>()
    private lateinit var filtered: FilteredList<BotRow>

    // map botId -> row
    private val rowById = ConcurrentHashMap<String, BotRow>()

    // map botId -> job that listens to runner.state
    private val stateJobById = ConcurrentHashMap<String, Job>()

    // selected bot subscriptions
    private var selectedJob: Job? = null

    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val zone = ZoneId.systemDefault()

    @FXML
    fun initialize() {
        setupTable()
        setupFilters()
        setupActions()

        // bind fleet (list of bots)
        bindFleet()
    }

    private fun setupTable() {
        colBotName.setCellValueFactory { it.value.nameProperty }
        colSymbol.setCellValueFactory { it.value.symbolProperty }
        colStatus.setCellValueFactory { it.value.statusProperty }
        colMode.setCellValueFactory { it.value.modeProperty }

        filtered = FilteredList(rows) { true }
        botsTable.items = filtered

        botsTable.selectionModel.selectedItemProperty().addListener { _, _, row ->
            bindSelectedBot(row?.botId)
        }
    }

    private fun setupFilters() {
        modeFilterCombo.items.setAll("ALL", Mode.LIVE.label, Mode.SANDBOX.label)
        modeFilterCombo.selectionModel.selectFirst()

        statusFilterCombo.items.setAll("ALL", "IDLE", "ANALYZING", "RUNNING", "PAUSED", "STOPPED", "ERROR")
        statusFilterCombo.selectionModel.selectFirst()

        fun apply() {
            val q = searchField.text.orEmpty().trim().uppercase()
            val mode = modeFilterCombo.value ?: "ALL"
            val st = statusFilterCombo.value ?: "ALL"

            filtered.setPredicate { r ->
                val okQ = q.isEmpty() ||
                        r.nameProperty.get().uppercase().contains(q) ||
                        r.symbolProperty.get().uppercase().contains(q)

                val okM = mode == "ALL" || r.modeProperty.get() == mode
                val okS = st == "ALL" || r.statusProperty.get() == st

                okQ && okM && okS
            }

            lblFleetCount.text = "${filtered.size} bots"
            lblActiveBots.text = "Active bots: ${rows.count { it.statusProperty.get() == "RUNNING" }}"
        }

        searchField.textProperty().addListener { _, _, _ -> apply() }
        modeFilterCombo.valueProperty().addListener { _, _, _ -> apply() }
        statusFilterCombo.valueProperty().addListener { _, _, _ -> apply() }
        apply()
    }

    private fun setupActions() {
        logArea.isEditable = false
        chkAutoScroll.isSelected = true

        btnClearLog.setOnAction { logArea.clear() }

        btnStart.setOnAction {
            val id = botsTable.selectionModel.selectedItem?.botId ?: return@setOnAction
            uiScope.launch { registry.get(id)?.start() }
        }
        btnPause.setOnAction {
            val id = botsTable.selectionModel.selectedItem?.botId ?: return@setOnAction
            uiScope.launch { registry.get(id)?.pause() }
        }
        btnStop.setOnAction {
            val id = botsTable.selectionModel.selectedItem?.botId ?: return@setOnAction
            uiScope.launch { registry.get(id)?.stop() }
        }
        btnDeployBot.setOnAction { openDeployWizard() }
    }

    private fun bindFleet() {
        uiScope.launch {
            registry.runners.collect { list ->
                // 1) add new
                val ids = list.map { it.botId }.toSet()

                list.forEach { runner ->
                    if (!rowById.containsKey(runner.botId)) {
                        val st = runner.state.value
                        val row = BotRow(
                            botId = runner.botId,
                            name = st.name,
                            symbol = st.symbol,
                            mode = st.mode.label,
                            status = st.status.name
                        ).also {
                            it.statusEnumProperty.set(st.status)
                        }

                        rowById[runner.botId] = row
                        rows.add(row)

                        // start listening to state updates for this runner
                        stateJobById[runner.botId] = launch {
                            runner.state.collect { s -> updateRowFromState(s) }
                        }
                    }
                }

                // 2) remove missing
                val toRemove = rowById.keys.filter { it !in ids }
                toRemove.forEach { id ->
                    stateJobById.remove(id)?.cancel()
                    val row = rowById.remove(id)
                    if (row != null) rows.remove(row)
                }

                // 3) auto select first if nothing selected
                if (botsTable.selectionModel.selectedItem == null && rows.isNotEmpty()) {
                    botsTable.selectionModel.selectFirst()
                }
            }
        }
    }

    private fun updateRowFromState(s: BotState) {
        val row = rowById[s.botId] ?: return

        // JavaFx dispatcher already, safe to set properties
        row.nameProperty.set(s.name)
        row.symbolProperty.set(s.symbol)
        row.modeProperty.set(s.mode.label)
        row.statusProperty.set(s.status.name)
        row.statusEnumProperty.set(s.status)

        row.pnlUsdProperty.set(money(s.metrics.totalPnlUsd))
        row.tradesProperty.set(s.metrics.trades)
        row.updatedProperty.set(s.metrics.lastActionAt?.let { timeFmt.format(it.atZone(zone)) } ?: "--")

        // if this bot is selected, update detail too (cheap check)
        val selectedId = botsTable.selectionModel.selectedItem?.botId
        if (selectedId == s.botId) {
            updateDetail(s)
            updateActionButtons(s.status)
        }
    }

    private fun bindSelectedBot(botId: String?) {
        selectedJob?.cancel()
        if (botId == null) return

        val runner = registry.get(botId) ?: return
        logArea.clear()

        // initial paint
        updateDetail(runner.state.value)
        updateActionButtons(runner.state.value.status)

        selectedJob = uiScope.launch {
            launch {
                runner.logs.collect { e ->
                    appendLog(e)
                }
            }
            launch {
                runner.state.collect { s ->
                    updateDetail(s)
                    updateActionButtons(s.status)
                }
            }
        }
    }

    private fun updateDetail(s: BotState) {
        lblBotTitle.text = s.name
        lblBotStatusBadge.text = s.status.name
        lblBotSubtitle.text = "Uptime: ${formatUptime(s.metrics.uptimeSec)}"

        lblTotalPnl.text = money(s.metrics.totalPnlUsd)
        lblPnlHint.text = "${pct(s.metrics.pnlTodayPct)} today"

        lblWinRate.text = pct(s.metrics.winRate * 100)
        lblTrades.text = "Trades: ${s.metrics.trades}"

        lblExposure.text = s.metrics.exposureText
        lblPositions.text = "Positions: ${s.metrics.positions}"

        lblHealth.text = "${s.metrics.healthScore}/100"
        lblLatency.text = "Latency: ${s.metrics.latencyMs}ms"
    }

    private fun updateActionButtons(status: BotStatus) {
        when (status) {
            BotStatus.RUNNING -> {
                btnStart.isDisable = true
                btnPause.isDisable = false
                btnStop.isDisable = false
            }

            BotStatus.PAUSED -> {
                btnStart.isDisable = false
                btnPause.isDisable = true
                btnStop.isDisable = false
            }

            BotStatus.STOPPED, BotStatus.IDLE -> {
                btnStart.isDisable = false
                btnPause.isDisable = true
                btnStop.isDisable = true
            }

            BotStatus.ANALYZING -> {
                btnStart.isDisable = true
                btnPause.isDisable = true
                btnStop.isDisable = false
            }

            BotStatus.ERROR -> {
                btnStart.isDisable = false
                btnPause.isDisable = true
                btnStop.isDisable = true
            }
        }
    }

    private fun appendLog(e: LogEvent) {
        val ts = timeFmt.format(e.ts.atZone(zone))
        logArea.appendText("[$ts] [${e.level}] ${e.message}\n")
        if (chkAutoScroll.isSelected) logArea.positionCaret(logArea.length)
    }

    private fun money(v: Double): String {
        val sign = if (v >= 0) "" else "-"
        val abs = kotlin.math.abs(v)
        val s = (abs * 100).roundToInt() / 100.0
        return "${sign}$${"%.2f".format(s)}"
    }

    private fun pct(v: Double): String {
        val s = (v * 10).roundToInt() / 10.0
        return "${"%.1f".format(s)}%"
    }

    private fun formatUptime(sec: Long): String {
        if (sec <= 0) return "--"
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }
    private fun openDeployWizard() {
        val loader = FXMLLoader(javaClass.getResource("/deploy_bot_wizard.fxml"))
        val root = loader.load<javafx.scene.Parent>()
        val ctrl = loader.getController<DeployBotWizardController>()

        val dialog = Stage().apply {
            initModality(Modality.APPLICATION_MODAL)
            title = "Deploy Bot"
            scene = Scene(root).apply {
                // nếu có css global thì add ở đây
                // stylesheets.add(javaClass.getResource("/com/automoney/ui/app.css")!!.toExternalForm())
            }
            isResizable = false
        }

        ctrl.onCancel = { dialog.close() }
        ctrl.onCreate = { cfg ->
            val row = BotRow(
                botId = cfg.botId,
                name = cfg.name,
                symbol = cfg.symbol,
                mode = cfg.mode.name,
                status = "STOPPED"
            )

            rows.add(row)
            botsTable.selectionModel.select(row)

            dialog.close()
        }

        dialog.showAndWait()
    }
}
