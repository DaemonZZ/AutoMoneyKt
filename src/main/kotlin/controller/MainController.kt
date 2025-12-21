package com.daemonz.controller

import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.TextField

class MainController {
    @FXML private lateinit var nameField: TextField
    @FXML private lateinit var resultLabel: Label

    @FXML
    fun onHello() {
        val name = nameField.text.trim().ifEmpty { "bạn" }
        resultLabel.text = "Xin chào, $name!"
    }
}
