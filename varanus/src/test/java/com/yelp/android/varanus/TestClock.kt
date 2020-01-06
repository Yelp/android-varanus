package com.yelp.android.varanus

import com.google.android.gms.common.util.Clock

class TestClock : Clock {
    var time = 10L
    private var nanoTime = 10_000L
    override fun currentTimeMillis() = time
    override fun elapsedRealtime() = time
    override fun currentThreadTimeMillis() = time
    override fun nanoTime() = nanoTime
}
