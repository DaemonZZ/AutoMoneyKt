package com.daemonz.controller.dialog

import com.daemonz.base.BaseController
import com.daemonz.utils.SessionAuth
import com.daemonz.utils.SystemConfig
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Dialog
import javafx.scene.control.TextField

class AuthenticationDialogController : BaseController() {
    lateinit var applyBtn: Button
    lateinit var tfApiSecret: TextField
    lateinit var tfApiKey: TextField

    private var dialog: Dialog<Boolean>? = null

    fun attach(dialog: Dialog<Boolean>) {
        this.dialog = dialog
    }

    override fun initUi() {
        SystemConfig.auth.let {
            tfApiKey.text = it.apiKey
            tfApiSecret.text = it.secret
        }
        applyBtn.setOnAction {
            val auth = SessionAuth(tfApiKey.text, tfApiSecret.text)
            SystemConfig.auth = auth
            dialog?.result = true
            dialog?.close()
        }
    }

    override fun fetchData() {
    }

    override fun setupObserver() {
    }
}