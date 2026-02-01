package com.daemonz.controller

import com.daemonz.base.BaseController
import com.daemonz.core.engine.BacktestResult
import com.daemonz.core.engine.EquityPoint
import com.daemonz.runtime.scanner.ScanResult
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView

class TradeDetailController : BaseController() {

    lateinit var colDuration: TableColumn<Any, Any>
    lateinit var colExitPrice: TableColumn<Any, Any>
    lateinit var colPnl: TableColumn<Any, Any>
    lateinit var colEntryPrice: TableColumn<Any, Any>
    lateinit var colEntryTime: TableColumn<Any, Any>
    lateinit var colSide: TableColumn<Any, Any>
    lateinit var tradesTable: TableView<Any>
    lateinit var equityYAxis: NumberAxis
    lateinit var equityXAxis: NumberAxis
    lateinit var equityChart: LineChart<Number, Number>
    lateinit var maxDdLabel: Label
    lateinit var pfLabel: Label
    lateinit var winRateLabel: Label
    lateinit var tradesLabel: Label
    lateinit var scoreLabel: Label
    lateinit var verdictLabel: Label
    lateinit var strategyLabel: Label
    lateinit var symbolLabel: Label
    lateinit var titleLabel: Label

    /**
     * Load trade detail data.
     * Must be called once after FXML loaded.
     */
    fun load(
        scan: ScanResult,
        backtest: BacktestResult
    ) {
        // đảm bảo chạy trên FX thread
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater { load(scan, backtest) }
            return
        }

        /* ===== Summary ===== */
        titleLabel.text = "Trade Detail – ${scan.symbol}"
        symbolLabel.text = scan.symbol
        strategyLabel.text = "EMA Pullback v7"
        verdictLabel.text = scan.verdict.name
        scoreLabel.text = scan.score.toString()

        tradesLabel.text = scan.metrics.trades.toString()
        winRateLabel.text = "%.1f%%".format(scan.metrics.winRate * 100)
        pfLabel.text = "%.2f".format(scan.metrics.profitFactor)
        maxDdLabel.text = "%.1f%%".format(scan.metrics.maxDrawdownPct)

        /* ===== Equity Curve ===== */
        buildEquityCurve(backtest.equityCurve)

        /* ===== Trades Table ===== */
        tradesTable.items = FXCollections.observableArrayList(backtest.trades)
    }

    private fun buildEquityCurve(curve: List<EquityPoint>) {
        equityChart.data.clear()
        if (curve.isEmpty()) return

        val series = XYChart.Series<Number, Number>().apply { name = "Equity" }
        curve.forEachIndexed { idx, p ->
            series.data.add(XYChart.Data(idx, p.equity))
        }

        equityXAxis.label = "Bar"
        equityXAxis.isForceZeroInRange = false
        equityYAxis.isForceZeroInRange = false

        equityChart.data.add(series)
    }
}