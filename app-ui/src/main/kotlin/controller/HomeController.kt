package com.daemonz.controller

import com.daemonz.adapters.binance.BinanceConfig
import com.daemonz.adapters.binance.BinanceFuturesAdapter
import com.daemonz.base.BaseController
import com.daemonz.controller.dialog.AuthenticationDialogController
import com.daemonz.models.AccountSnapshot
import com.daemonz.runtime.AccountService
import com.daemonz.runtime.Mode
import com.daemonz.utils.SystemConfig
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.control.Button
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.layout.GridPane
import javafx.util.StringConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeController : BaseController() {
    @FXML
    lateinit var lbMarginMode: Label

    @FXML
    lateinit var tfAvailable: Label

    @FXML
    lateinit var lbMargin: Label

    @FXML
    lateinit var lbDeposit: Label

    @FXML
    lateinit var lbWithdraw: Label

    @FXML
    lateinit var lbTrade: Label

    @FXML
    lateinit var modeChoice: ChoiceBox<Mode>

    @FXML
    lateinit var lbStatus: Label

    @FXML
    lateinit var pnlLabel: Label

    @FXML
    lateinit var balanceLb: Label

    @FXML
    lateinit var testBtn: Button

    @FXML
    lateinit var authBtn: Button


    override fun initUi() {
        authBtn.setOnAction {
            val rs = showAuthDialog()
            if (rs) refreshData()
        }
        testBtn.setOnAction {
            refreshData()
        }
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

    override fun fetchData() {

    }

    private fun refreshData() {
        uiScope.launch {
            val result = withContext(Dispatchers.IO) {
                val cfg = BinanceConfig(
                    apiKey = SystemConfig.auth.apiKey,
                    apiSecret = SystemConfig.auth.secret,
                    baseUrl = SystemConfig.mode.baseUrl
                )
                val exchange = BinanceFuturesAdapter(cfg)
                val service = AccountService(exchange)
                val status = service.checkConnection()
                if (!status.ok) return@withContext AccountSnapshot(false, status.message)
                val balances = service.loadBalances()
                val accountInfo = service.getAccountInfo()
                return@withContext AccountSnapshot(true, "OK", accountInfo, balances)

            }
            if (!result.connectionOk) {
                println("❌ ${result.message}")
                lbStatus.text = "❌ Disconnected"
            } else {
                println("✅ Connected ")
                println(result.balances)
                println(result.accountInfo)
                lbStatus.text = "✅ Connected "
                balanceLb.text = result.accountInfo?.totalAvailableBalance.toString()
            }
        }
    }

    private fun showAuthDialog(): Boolean {
        val dialog = Dialog<Boolean>()
        dialog.title = "Authentication"
        val loader = FXMLLoader(javaClass.getResource("/dialog_auth_info.fxml"))
        val content = loader.load<GridPane>()
        val controller = loader.getController<AuthenticationDialogController>()
        controller.attach(dialog)
        dialog.dialogPane.content = content
        dialog.dialogPane.buttonTypes.clear()
        return dialog.showAndWait().orElse(false)
    }

    override fun setupObserver() {
    }
}