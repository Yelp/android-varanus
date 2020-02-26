package com.yelp.android.varanus

import com.yelp.android.varanus.LogUploadingManager.LogUploaderBase
import com.yelp.android.varanus.NetworkTrafficLogPersister.TrafficLogSummary
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class EndpointSpecificNetworkTrackerTest {

    private val networkTrafficPersister = TestNetworkTrafficLogPersister()
    private val alertIssuer = TestLogUploader()
    private val window = TimeUnit.MINUTES.toMillis(5)

    @Test
    fun testTracksRequests() {
        testTracksRequestsResponses(true, 5, 50L)
    }

    @Test
    fun testTrackResponses() {
        testTracksRequestsResponses(false, 0, 50L)
    }

    private fun testTracksRequestsResponses(
        isRequest: Boolean,
        expectedCount: Int,
        expectedSize: Long
    ) {
        val tracker = EndpointSpecificNetworkTracker("test",
                TimeUnit.SECONDS.toMillis(5),
                networkTrafficPersister,
                LogUploadingManager(alertIssuer, window, 5L))

        for (i in 1..5) {
            tracker.addLogAndPersist(NetworkTrafficLog(isRequest, "test", "test", 10))
        }
        assertEquals(tracker.requestCount.get(), expectedCount + 1)
        assertEquals(tracker.requestSize.get(), expectedSize + 1)
    }


    class TestNetworkTrafficLogPersister : NetworkTrafficLogPersister {
        override fun getSizeAndClear(windowLength: Long, endpoint: String): TrafficLogSummary {
            return TrafficLogSummary(1, 1)
        }
        override fun clear(windowLength: Long, endpoint: String) {}

        override fun addLog(log: NetworkTrafficLog) {}
    }

    class TestLogUploader : LogUploaderBase {
        var counter = 0

        override suspend fun uploadTrafficLogSummaryForInterval(
            data: Long,
            requests: Int,
            time_interval: Long,
            endpoint: String
        ) {
            counter++
        }
    }

}
