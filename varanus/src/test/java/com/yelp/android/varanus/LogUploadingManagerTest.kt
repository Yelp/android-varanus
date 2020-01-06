package com.yelp.android.varanus

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class LogUploadingManagerTest {

    private val persister = TestNetworkTrafficLogPersister()
    private val windowSize = TimeUnit.SECONDS.toMillis(9)

    private lateinit var testClock: TestClock
    private lateinit var alertIssuer: TestLogUploader
    private lateinit var trafficAlerter: LogUploadingManager
    private lateinit var endpoints: Map<String, EndpointSpecificNetworkTracker>
    private val CLEAR_INCREMENT = windowSize + 1

    @Before
    fun setup() {
        testClock = TestClock()
        alertIssuer = TestLogUploader()
        trafficAlerter = LogUploadingManager(alertIssuer, windowSize, windowSize)
                .also { it.setClockForTesting(testClock) }
        endpoints = mapOf("test" to EndpointSpecificNetworkTracker("test", windowSize, persister, trafficAlerter),
                "test2" to EndpointSpecificNetworkTracker("test2", windowSize, persister, trafficAlerter))
    }

    @Test
    fun testOneLogSent_getsRecorded() {
        addRequest()

        assertEquals(2, alertIssuer.sentLogs.size) // 1 for test, 1 for total
        assertEquals(1, alertIssuer.sentLogs[0].requests)
        assertEquals(2, alertIssuer.sentLogs[0].data)
        assertEquals(1, alertIssuer.sentLogs[1].requests)
        assertEquals(2, alertIssuer.sentLogs[1].data)
    }

    @Test
    fun testAlertsOverTime_triggeredAfterTimeExpiresOnly() {
        addRequest()

        assertEquals(2, alertIssuer.sentLogs.size)

        testClock.time += CLEAR_INCREMENT / 2
        addRequest()

        // No alerts or periodic stats should have been sent because the timer didn't expire
        // Still 1 log + 1 total
        assertEquals(2, alertIssuer.sentLogs.size)

        // This exceeds the timers and should send 2 logs
        testClock.time += CLEAR_INCREMENT
        addRequest()
        assertEquals(4, alertIssuer.sentLogs.size)
    }

    @Test
    fun testTwoEndpointAlerts_sendsAtCorrectTimes() {
        addRequest()

        // There are now 2 sentAlerts and 1 sentLogs
        testClock.time += CLEAR_INCREMENT / 2
        addRequest("test2")

        // There are now two new alerts pending but not sent yet
        assertEquals(2, alertIssuer.sentLogs.size)

        // This exceeds the timers and should send 2 more alerts and another log for each endpoint,
        // plus flush the alerts for endpoint_2.
        testClock.time += CLEAR_INCREMENT
        addRequest()
        // 1 log for the first request, 1 log for the first total
        // 2 logs for each of the subsequent endpoints and one for the overall total
        assertEquals(5, alertIssuer.sentLogs.size)
    }

    private fun addRequest(name: String = "test") {
        endpoints[name]?.addLogAndPersist(NetworkTrafficLog(true, "test", "test", 2))

        runBlocking {
            trafficAlerter.registerLogs(HashMap(endpoints))
        }
    }

    class TestNetworkTrafficLogPersister : NetworkTrafficLogPersister {
        override fun getSizeAndClear(
            windowLength: Long,
            endpoint: String
        ): NetworkTrafficLogPersister.TrafficLogSummary {
            return NetworkTrafficLogPersister.TrafficLogSummary(0, 0)
        }

        override fun clear(windowLength: Long, endpoint: String) {}

        override fun addLog(log: NetworkTrafficLog) {}
    }

}
