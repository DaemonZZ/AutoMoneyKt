package com.daemonz.controller

import com.daemonz.adapters.binance.BinanceConfig
import com.daemonz.adapters.binance.BinanceFuturesAdapter
import com.daemonz.base.BaseController
import com.daemonz.controller.dialog.AuthenticationDialogController
import com.daemonz.models.AccountSnapshot
import com.daemonz.runtime.AccountService
import com.daemonz.utils.SystemConfig
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.control.Button
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.layout.GridPane
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeController : BaseController() {
    lateinit var lbStatus: Label
    lateinit var pnlLabel: Label
    lateinit var balanceLb: Label
    lateinit var testBtn: Button
    lateinit var authBtn: Button

    @FXML
    fun initialize() {
        authBtn.setOnAction {
            val rs = showAuthDialog()
            if (rs) refreshData()
        }
        testBtn.setOnAction {
            refreshData()
        }
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
}