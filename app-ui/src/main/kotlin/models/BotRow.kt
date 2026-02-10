package com.daemonz.models

import com.daemonz.runtime.bot.BotStatus
import javafx.beans.property.*

class BotRow(
    botId: String,
    name: String,
    symbol: String,
    mode: String,
    status: BotStatus
) {
    val botIdProperty = SimpleStringProperty(botId)
    val nameProperty = SimpleStringProperty(name)
    val symbolProperty = SimpleStringProperty(symbol)
    val modeProperty = SimpleStringProperty(mode)
    val statusProperty = SimpleStringProperty(status.name)

    // optional: for sort/filter later
    val statusEnumProperty = SimpleObjectProperty<BotStatus>(BotStatus.IDLE)

    val pnlUsdProperty = SimpleStringProperty("--")
    val tradesProperty = SimpleIntegerProperty(0)
    val updatedProperty = SimpleStringProperty("--")

    val botId: String get() = botIdProperty.get()
}
