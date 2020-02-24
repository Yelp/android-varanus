package com.yelp.android.varanus.shutoff

import java.util.concurrent.TimeUnit

object TestConfig {

    val config = NetworkShutoffManager.Config(
            TimeUnit.MINUTES.toMillis(5),
            8, // 8 * 5 = 40 minute,
            10, // drop 9 in 10 requests when trying again,
            556,
            555,
            TimeUnit.MINUTES.toMillis(5))
}
