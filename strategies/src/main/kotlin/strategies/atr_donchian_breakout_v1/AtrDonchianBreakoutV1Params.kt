package com.daemonz.strategies.atr_donchian_breakout_v1

data class AtrDonchianBreakoutV1Params(
    val donchianLen: Int = 20,
    val atrLen: Int = 14,

    // breakout must exceed channel by this ATR multiple
    val breakoutAtrMult: Double = 0.7,

    // SL distance in ATR
    val slAtrMult: Double = 2.4,

    // TP in R-multiple (tp = entry + tpR * riskDistance). If <= 0, we simulate "no TP" by setting a very far TP.
    val tpR: Double = 2.0,

    // volatility filter (ATR% = ATR/close*100)
    val minAtrPct: Double = 0.3,
    val maxAtrPct: Double = 3.0,

    // safety: avoid ultra-tight SL
    val minSlDistancePct: Double = 0.05, // 0.05% of price

    // v1.1 upgrades
    val cooldownBarsAfterExit: Int = 5,
    val minChannelWidthPct: Double = 0.9,   // (upper-lower)/close*100
    val rearmOnInsideChannel: Boolean = true,

    // v1.2: ATR Expansion (volatility must be expanding)
    val atrExpansionLen: Int = 20,          // SMA length of ATR
    val atrExpansionMult: Double = 1.05     // ATR_now >= ATR_SMA * mult

)
