package com.yelp.android.varanus

import com.yelp.android.varanus.LogUploadingManager.LogUploaderBase

class TestLogUploader : LogUploaderBase {
    var sentLogs = ArrayList<PeriodicLog>()

    override suspend fun uploadTrafficLogSummaryForInterval(
            data: Long,
            requests: Int,
            time_interval: Long,
            endpoint: String) {
        sentLogs.add(PeriodicLog(data, requests, endpoint))
    }

    data class PeriodicLog(val data: Long, val requests: Int, val endpoint: String)
}
