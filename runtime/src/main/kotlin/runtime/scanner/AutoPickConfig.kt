package com.daemonz.runtime.scanner

enum class AutoPickLogic {
    TOP_BY_VOLUME,
    TOP_GAINERS_24H,
    RANDOM_DIVERSITY
}

data class AutoPickConfig(
    val count: Int = 5,
    val poolSize: Int = 60,              // analyze tối thiểu bao nhiêu symbol để tìm ra 5 TRADE
    val maxPoolSize: Int = 300,          // giới hạn để không chạy quá lâu
    val stepSize: Int = 60,              // nếu chưa đủ TRADE thì tăng pool thêm bao nhiêu
    val logic: AutoPickLogic = AutoPickLogic.TOP_BY_VOLUME,
    val excludeStablecoins: Boolean = true,
    val excludeLeveragedTokens: Boolean = true
)
