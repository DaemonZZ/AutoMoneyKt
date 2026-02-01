package com.daemonz.controller

import com.daemonz.adapters.binance.BinanceConfig
import com.daemonz.adapters.binance.BinanceFuturesAdapter
import com.daemonz.base.BaseController
import com.daemonz.core.engine.BacktestResult
import com.daemonz.core.market.Timeframe
import com.daemonz.core.risk.RiskConfig
import com.daemonz.core.strategy.StrategySpec
import com.daemonz.runtime.Mode
import com.daemonz.runtime.scanner.*
import com.daemonz.runtime.status.AppStatus
import com.daemonz.strategies.atr_donchian_breakout_v1.AtrDonchianBreakoutV1Compatibility
import com.daemonz.strategies.atr_donchian_breakout_v1.AtrDonchianBreakoutV1CompatibilityParams
import com.daemonz.strategies.ema_pullback_v7.EmaPullbackV7Compatibility
import com.daemonz.strategies.ema_pullback_v7.EmaPullbackV7CompatibilityParams
import com.daemonz.strategies.registry.StrategyRegistry
import com.daemonz.strategies.registry.StrategySelection
import com.daemonz.strategies.registry.newSelectionAny
import com.daemonz.utils.SystemConfig
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.transformation.FilteredList
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.stage.Modality
import javafx.stage.Stage
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

private data class AnalyzeCacheKey(
    val symbol: String,
    val strategyKey: String,
    val interval: Timeframe,
    val candleLimit: Int,
    val startingEquity: Double
)

class MarketAnalyzerController : BaseController() {

    /* =========================
       UI – Right filters
       ========================= */
    @FXML lateinit var applyFiltersCheck: CheckBox

    @FXML lateinit var colVolPct: TableColumn<ScanResult, String>
    @FXML lateinit var autoPickBtn: Button

    /* =========================
           UI – Top bar
       ========================= */
    @FXML lateinit var strategyCombo: ComboBox<StrategySpec<*>>
    @FXML lateinit var intervalCombo: ComboBox<Timeframe>
    @FXML lateinit var historyCombo: ComboBox<Int>
    @FXML lateinit var modeCombo: ComboBox<String>
    @FXML lateinit var riskField: TextField
    @FXML lateinit var equityField: TextField

    /* =========================
       UI – Left panel
       ========================= */
    @FXML lateinit var symbolSearchField: TextField
    @FXML lateinit var symbolListView: ListView<String>
    @FXML lateinit var selectedCountLabel: Label
    @FXML lateinit var clearSelectionBtn: Button

    /* =========================
       UI – Center table
       ========================= */
    @FXML lateinit var resultsTable: TableView<ScanResult>
    @FXML lateinit var resultsCountLabel: Label

    @FXML lateinit var colSymbol: TableColumn<ScanResult, String>
    @FXML lateinit var colVerdict: TableColumn<ScanResult, String>
    @FXML lateinit var colScore: TableColumn<ScanResult, Number>
    @FXML lateinit var colTrades: TableColumn<ScanResult, Number>
    @FXML lateinit var colWinRate: TableColumn<ScanResult, String>
    @FXML lateinit var colPf: TableColumn<ScanResult, String>
    @FXML lateinit var colMaxDd: TableColumn<ScanResult, String>

    /* =========================
       UI – Right panel
       ========================= */
    @FXML lateinit var analyzeBtn: Button
    @FXML lateinit var cancelBtn: Button
    @FXML lateinit var progressBar: ProgressBar
    @FXML lateinit var statusLabel: Label
    @FXML lateinit var progressTextLabel: Label

    // Filters
    @FXML private lateinit var minTradesSlider: Slider
    @FXML private lateinit var minTradesValueLabel: Label
    @FXML private lateinit var minScoreSlider: Slider
    @FXML private lateinit var minScoreValueLabel: Label
    @FXML private lateinit var minVolatilityField: TextField
    @FXML private lateinit var hideLowSampleCheck: CheckBox

    /* =========================
       Runtime
       ========================= */
    private var runningJob: Job? = null

    private val allSymbols = FXCollections.observableArrayList<String>()
    private lateinit var filteredSymbols: FilteredList<String>

    // ✅ checkbox state
    private val selectedMap = ConcurrentHashMap<String, BooleanProperty>()

    // source toggle
    @FXML private lateinit var manualToggle: ToggleButton
    @FXML private lateinit var autoPickToggle: ToggleButton

    @FXML private lateinit var autoPickTopVolumeRadio: RadioButton
    @FXML private lateinit var autoPickRandomRadio: RadioButton
    @FXML private lateinit var autoPickGainersRadio: RadioButton

    private val sourceGroup = ToggleGroup()
    private val autoPickGroup = ToggleGroup()

    private val resultsSource = FXCollections.observableArrayList<ScanResult>()
    private lateinit var filteredResults: FilteredList<ScanResult>

    private val backtestCache = ConcurrentHashMap<AnalyzeCacheKey, BacktestResult>()
    private val volatilityCache = ConcurrentHashMap<AnalyzeCacheKey, Double>()

    private data class AnalyzeKeyPrefix(
        val strategyKey: String,
        val interval: Timeframe,
        val candleLimit: Int,
        val startingEquity: Double
    )

    private var activePrefix: AnalyzeKeyPrefix? = null

    // Strategy selection
    private val selectionCache = mutableMapOf<com.daemonz.core.strategy.StrategyId, StrategySelection>()
    private var currentSelection: StrategySelection? = null

    /* =========================
       Init
       ========================= */
    @FXML
    fun initialize() {
        setupTopBar()
        setupSymbolList() // ✅ checkbox list
        setupTable()
        setupFilters()

        analyzeBtn.setOnAction { startAnalyze() }
        cancelBtn.setOnAction { runningJob?.cancel() }

        setStatus(AppStatus.IDLE)
        progressBar.progress = 0.0
        progressTextLabel.text = "0/0"

        manualToggle.toggleGroup = sourceGroup
        autoPickToggle.toggleGroup = sourceGroup
        manualToggle.isSelected = true

        autoPickTopVolumeRadio.toggleGroup = autoPickGroup
        autoPickRandomRadio.toggleGroup = autoPickGroup
        autoPickGainersRadio.toggleGroup = autoPickGroup
        autoPickTopVolumeRadio.isSelected = true

        uiScope.launch { loadSymbols() }
    }

    /* =========================
       UI setup
       ========================= */
    private fun setupTopBar() {
        strategyCombo.items = FXCollections.observableArrayList(StrategyRegistry.all)
        strategyCombo.selectionModel.selectFirst()
        strategyCombo.setCellFactory {
            object : ListCell<StrategySpec<*>>() {
                override fun updateItem(item: StrategySpec<*>?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) "" else item.displayName
                }
            }
        }
        strategyCombo.buttonCell = object : ListCell<StrategySpec<*>>() {
            override fun updateItem(item: StrategySpec<*>?, empty: Boolean) {
                super.updateItem(item, empty)
                text = if (empty || item == null) "" else item.displayName
            }
        }

        fun ensureSelectionFor(spec: StrategySpec<*>): StrategySelection {
            val sel = selectionCache.getOrPut(spec.id) { spec.newSelectionAny() }
            currentSelection = sel
            return sel
        }

        strategyCombo.valueProperty().addListener { _, _, newSpec ->
            if (newSpec != null) ensureSelectionFor(newSpec)
        }
        ensureSelectionFor(strategyCombo.value ?: StrategyRegistry.all.first())

        intervalCombo.items.setAll(Timeframe.entries.toList())
        intervalCombo.selectionModel.select(Timeframe.M15)

        historyCombo.items = FXCollections.observableArrayList(500, 1000, 1500)
        historyCombo.selectionModel.selectLast()

        modeCombo.items = FXCollections.observableArrayList(Mode.entries.map { it.label })
        modeCombo.selectionModel.select(SystemConfig.mode.label)

        riskField.text = "1.0"
        equityField.text = "10000"
    }

    /**
     * ✅ Symbol list as checkbox (fix: stable binding with cell reuse)
     */
    private fun setupSymbolList() {
        filteredSymbols = FilteredList(allSymbols) { true }
        symbolListView.items = filteredSymbols
        symbolListView.selectionModel.selectionMode = SelectionMode.SINGLE

        allSymbols.addListener(ListChangeListener<String> { ch ->
            while (ch.next()) {
                if (ch.wasAdded()) ch.addedSubList.forEach { ensurePropFor(it) }
            }
            updateSelectedCount()
        })

        symbolListView.setCellFactory {
            object : ListCell<String>() {
                private val cb = CheckBox()
                private var boundProp: BooleanProperty? = null

                init {
                    // click row (not checkbox) toggles too
                    setOnMouseClicked { e ->
                        val sym = item ?: return@setOnMouseClicked
                        if (e.clickCount == 1 && e.target !is CheckBox) {
                            val p = ensurePropFor(sym)
                            p.set(!p.get())
                        }
                    }
                }

                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)

                    // unbind old property first (cell reuse fix)
                    boundProp?.let { old ->
                        cb.selectedProperty().unbindBidirectional(old)
                    }
                    boundProp = null

                    if (empty || item == null) {
                        cb.text = null
                        graphic = null
                        text = null
                        return
                    }

                    cb.text = item
                    val newProp = ensurePropFor(item)
                    cb.selectedProperty().bindBidirectional(newProp)
                    boundProp = newProp

                    graphic = cb
                    text = null
                }
            }
        }

        symbolSearchField.textProperty().addListener { _, _, q ->
            val key = q?.uppercase()?.trim().orEmpty()
            filteredSymbols.setPredicate { s -> key.isEmpty() || s.contains(key) }
        }

        clearSelectionBtn.setOnAction {
            selectedMap.values.forEach { it.set(false) }
            updateSelectedCount()
            symbolListView.refresh()
        }

        updateSelectedCount()
    }

    private fun ensurePropFor(symbol: String): BooleanProperty {
        return selectedMap.computeIfAbsent(symbol) {
            SimpleBooleanProperty(false).also { p ->
                p.addListener { _, _, _ -> updateSelectedCount() }
            }
        }
    }

    private fun updateSelectedCount() {
        val n = selectedMap.values.count { it.get() }
        selectedCountLabel.text = "Selected: $n"
    }

    private fun selectedSymbols(): List<String> =
        selectedMap.entries.filter { it.value.get() }.map { it.key }

    private fun setupTable() {
        colSymbol.setCellValueFactory { SimpleStringProperty(it.value.symbol) }
        colVerdict.setCellValueFactory { SimpleStringProperty(it.value.verdict.name) }
        colScore.setCellValueFactory { SimpleIntegerProperty(it.value.score) }
        colTrades.setCellValueFactory { SimpleIntegerProperty(it.value.metrics.trades) }

        colVolPct.setCellValueFactory { cell ->
            val vol = volatilityCache[keyForSymbol(cell.value.symbol)] ?: 0.0
            SimpleStringProperty("%.2f".format(vol))
        }

        colWinRate.setCellValueFactory {
            val v = (it.value.metrics.winRate * 1000).roundToInt() / 10.0
            SimpleStringProperty("$v%")
        }

        colPf.setCellValueFactory {
            SimpleStringProperty("%.2f".format(it.value.metrics.profitFactor))
        }

        colMaxDd.setCellValueFactory {
            val v = (it.value.metrics.maxDrawdownPct * 10).roundToInt() / 10.0
            SimpleStringProperty("$v%")
        }

        filteredResults = FilteredList(resultsSource) { true }
        resultsTable.items = filteredResults
        resultsCountLabel.text = "0 results"

        resultsTable.setRowFactory {
            val row = TableRow<ScanResult>()
            row.setOnMouseClicked { e ->
                if (e.clickCount == 2 && !row.isEmpty) openTradeDetail(row.item)
            }
            row
        }
    }

    private fun setupFilters() {
        minTradesSlider.min = 0.0
        minTradesSlider.max = 200.0
        minTradesSlider.value = 10.0
        minTradesValueLabel.text = "10"

        minScoreSlider.min = 0.0
        minScoreSlider.max = 100.0
        minScoreSlider.value = 50.0
        minScoreValueLabel.text = "50"

        if (minVolatilityField.text.isNullOrBlank()) minVolatilityField.text = "1.5"

        minTradesSlider.valueProperty().addListener { _, _, v ->
            minTradesValueLabel.text = v.toInt().toString()
            applyResultFilters()
        }
        minScoreSlider.valueProperty().addListener { _, _, v ->
            minScoreValueLabel.text = v.toInt().toString()
            applyResultFilters()
        }
        minVolatilityField.textProperty().addListener { _, _, _ -> applyResultFilters() }
        hideLowSampleCheck.selectedProperty().addListener { _, _, _ -> applyResultFilters() }

        applyFiltersCheck.selectedProperty().addListener { _, _, enabled ->
            minTradesSlider.isDisable = !enabled
            minScoreSlider.isDisable = !enabled
            minVolatilityField.isDisable = !enabled
            hideLowSampleCheck.isDisable = !enabled
            applyResultFilters()
        }
        applyResultFilters()
    }

    private fun applyResultFilters() {
        val enabled = applyFiltersCheck.isSelected

        val minTrades = minTradesSlider.value.toInt()
        val minScore = minScoreSlider.value.toInt()
        val hideLowSample = hideLowSampleCheck.isSelected
        val minVol = minVolatilityField.text.toDoubleOrNull() ?: 0.0

        val isStepB = resultsSource.isNotEmpty() && resultsSource.all { it.metrics.trades == 0 }

        filteredResults.setPredicate { r ->
            if (!enabled) return@setPredicate true
            if (r.score < minScore) return@setPredicate false

            val vol = volatilityCache[keyForSymbol(r.symbol)] ?: 0.0
            if (vol < minVol) return@setPredicate false

            if (isStepB) return@setPredicate true

            if (r.metrics.trades < minTrades) return@setPredicate false
            if (hideLowSample && r.metrics.trades < 10) return@setPredicate false

            true
        }

        resultsCountLabel.text =
            if (isStepB) "Shown ${filteredResults.size} / Raw ${resultsSource.size} (Step B)"
            else "Shown ${filteredResults.size} / Raw ${resultsSource.size}"
    }

    private fun snapshotPrefixFromUI(): AnalyzeKeyPrefix {
        val spec = strategyCombo.value ?: StrategyRegistry.all.first()
        val interval = intervalCombo.value ?: Timeframe.M15
        val limit = (historyCombo.value ?: 1500).coerceAtMost(1500)
        val equity = equityField.text.toDoubleOrNull() ?: 10_000.0

        return AnalyzeKeyPrefix(
            strategyKey = spec.id.value,
            interval = interval,
            candleLimit = limit,
            startingEquity = equity
        )
    }

    private fun keyForSymbol(symbol: String): AnalyzeCacheKey {
        val p = activePrefix ?: snapshotPrefixFromUI()
        return AnalyzeCacheKey(
            symbol = symbol,
            strategyKey = p.strategyKey,
            interval = p.interval,
            candleLimit = p.candleLimit,
            startingEquity = p.startingEquity
        )
    }

    private fun openTradeDetail(scan: ScanResult) {
        val key = keyForSymbol(scan.symbol)
        val bt = backtestCache[key]
        if (bt == null) {
            statusLabel.text = "No detail cached for ${scan.symbol}. Re-run Analyze."
            return
        }

        val loader = FXMLLoader(javaClass.getResource("/TradeDetailView.fxml"))
        val root = loader.load<Parent>()

        val controller = loader.getController<TradeDetailController>()
        controller.load(scan, bt)

        Stage().apply {
            title = "Trade Detail – ${scan.symbol}"
            scene = Scene(root, 900.0, 600.0)
            initModality(Modality.APPLICATION_MODAL)
            show()
        }
    }

    private suspend fun loadSymbols() {
        setStatus(AppStatus.LOADING_SYMBOLS)
        progressBar.progress = -1.0

        try {
            val exchange = buildExchange()
            val list = withContext(Dispatchers.IO) { exchange.listTradableSymbols() }

            allSymbols.setAll(list.sorted())
            allSymbols.forEach { ensurePropFor(it) }
            updateSelectedCount()

            setStatus(AppStatus.IDLE)
        } catch (e: Exception) {
            setStatus(AppStatus.ERROR)
            e.printStackTrace()
        } finally {
            progressBar.progress = 0.0
        }
    }

    private fun startAnalyze() {
        val useStepB = true
        if (runningJob?.isActive == true) return

        activePrefix = snapshotPrefixFromUI()

        volatilityCache.clear()
        backtestCache.clear()

        val selected = selectedSymbols()
        val interval = intervalCombo.value ?: Timeframe.M15
        val limit = (historyCombo.value ?: 1500).coerceAtMost(1500)
        val isAuto = autoPickToggle.isSelected

        val request = ScanRequest(
            symbols = if (isAuto) null else selected.ifEmpty { null },
            autoPickCount = 5,
            interval = interval,
            candleLimit = limit
        )

        val scanner = buildScannerService()
        val (strategy, params) = provideStrategyFromCombo()

        resultsSource.clear()
        analyzeBtn.isDisable = true
        setStatus(AppStatus.ANALYZING)
        progressBar.progress = 0.0
        progressTextLabel.text = "0/0"

        runningJob = uiScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {

                    val onProgressCb: suspend (Int, Int, String) -> Unit = { done, total, _ ->
                        withContext(Dispatchers.JavaFx) {
                            progressBar.progress = if (total > 0) done.toDouble() / total else 0.0
                            progressTextLabel.text = "$done/$total"
                        }
                    }

                    val onVolCb: suspend (String, Double) -> Unit = { symbol, volPct ->
                        volatilityCache[keyForSymbol(symbol)] = volPct
                    }

                    if (useStepB) {
                        runStepB(scanner, request, onProgressCb, onVolCb)
                    } else {
                        if (isAuto) {
                            val autoCfg = AutoPickConfig(
                                count = 5,
                                poolSize = 150,
                                maxPoolSize = 300,
                                stepSize = 60,
                                logic = when {
                                    autoPickTopVolumeRadio.isSelected -> AutoPickLogic.TOP_BY_VOLUME
                                    autoPickGainersRadio.isSelected -> AutoPickLogic.TOP_GAINERS_24H
                                    else -> AutoPickLogic.RANDOM_DIVERSITY
                                },
                                excludeStablecoins = false,
                                excludeLeveragedTokens = false
                            )

                            scanner.autoPickAnalyzeTopTrades(
                                req = request,
                                auto = autoCfg,
                                strategy = strategy,
                                params = params,
                                onProgress = onProgressCb,
                                onDetail = { symbol, bt -> backtestCache[keyForSymbol(symbol)] = bt },
                                onVolatility = onVolCb
                            )
                        } else {
                            scanner.analyze(
                                req = request,
                                strategy = strategy,
                                params = params,
                                onProgress = onProgressCb,
                                onVolatility = onVolCb
                            )
                        }
                    }
                }

                resultsSource.setAll(results)
                applyResultFilters()
                setStatus(AppStatus.DONE)

            } catch (_: CancellationException) {
                setStatus(AppStatus.CANCELLED)
            } catch (e: Exception) {
                setStatus(AppStatus.ERROR)
                e.printStackTrace()
            } finally {
                analyzeBtn.isDisable = false
                progressBar.progress = 0.0
            }
        }
    }

    private fun buildExchange(): com.daemonz.adapters.exchange.ExchangeAdapter {
        val auth = requireNotNull(SystemConfig.auth) { "Authentication required" }

        val cfg = BinanceConfig(
            apiKey = auth.apiKey,
            apiSecret = auth.secret,
            baseUrl = SystemConfig.mode.baseUrl
        )
        return BinanceFuturesAdapter(cfg)
    }

    private fun buildScannerService(): MarketScannerService {
        val equity = equityField.text.toDoubleOrNull() ?: 10_000.0
        val risk = RiskConfig(startingEquity = equity)
        return MarketScannerService(buildExchange(), risk)
    }

    private fun provideStrategyFromCombo(): Pair<com.daemonz.core.strategy.Strategy<Any>, Any> {
        val spec = strategyCombo.value ?: StrategyRegistry.all.first()
        val sel = currentSelection
            ?: selectionCache.getOrPut(spec.id) { spec.newSelectionAny() }.also { currentSelection = it }

        val params = sel.paramsAny()
        val strategy = sel.buildStrategyAny()
        return strategy to params
    }

    private fun setStatus(status: AppStatus) {
        statusLabel.text = status.label
        analyzeBtn.isDisable = status.isBusy
        progressBar.isVisible = status.isBusy
    }

    private suspend fun runStepB(
        scanner: MarketScannerService,
        request: ScanRequest,
        onProgress: suspend (done: Int, total: Int, symbol: String) -> Unit,
        onVolatility: suspend (symbol: String, volPct: Double) -> Unit
    ): List<ScanResult> {
        val spec = strategyCombo.value ?: StrategyRegistry.all.first()

        val cfg = StepBConfig(
            applyFilters = applyFiltersCheck.isSelected,
            maxConcurrency = 6,
            weightMarket = 0.5,
            weightCompat = 0.5
        )

        return when (spec.id.value) {
            "atr_donchian_breakout_v1" -> scanner.analyzeStepB(
                req = request,
                params = AtrDonchianBreakoutV1CompatibilityParams(),
                compat = AtrDonchianBreakoutV1Compatibility(),
                config = cfg,
                onProgress = onProgress,
                onVolatility = onVolatility
            )

            "ema_pullback_v7" -> scanner.analyzeStepB(
                req = request,
                params = EmaPullbackV7CompatibilityParams(),
                compat = EmaPullbackV7Compatibility(),
                config = cfg,
                onProgress = onProgress,
                onVolatility = onVolatility
            )

            else -> emptyList()
        }
    }
}
