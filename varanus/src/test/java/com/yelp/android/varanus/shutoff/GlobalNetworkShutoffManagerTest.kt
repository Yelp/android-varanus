package com.yelp.android.varanus.shutoff

import org.junit.Test
import kotlin.test.assertEquals

/**
 * This tests the functionality of shutting off all traffic for all endpoints.
 *
 * The functionality of shutting off traffic for just one endpoint is in
 * [PerEndpointNetworkShutoffManagerTest].
 */
class GlobalNetworkShutoffManagerTest : NetworkShutoffManagerTest() {

    private val config = TestConfig.config

    @Test
    fun testDefaultState_DoesntBlockTraffic() {
        checkIfDrop(DEFAULT_ENDPOINT, false)
    }

    @Test
    fun testAfter556Code_DropsTraffic() {
        setErrorCode(GLOBAL_FAIL_CODE)
        checkIfDrop(DEFAULT_ENDPOINT, true)
    }

    @Test
    fun testAfter556CodeAndSuccessfulRetry_NoLongerDropsTraffic() {
        setErrorCode(GLOBAL_FAIL_CODE)
        setClockToNextInterval()
        setErrorCode(SUCCESS_CODE)
        checkIfDrop(DEFAULT_ENDPOINT, false)
    }

    @Test
    fun testAfter556CodeAndFailedRetry_DropsTraffic() {
        setErrorCode(GLOBAL_FAIL_CODE)
        setClockToNextInterval()
        setErrorCode(GLOBAL_FAIL_CODE)
        checkIfDrop(DEFAULT_ENDPOINT, true)
    }

    /**
     * The expectation is that we will back off by 5 minutes at first, then an additional 5 minutes
     * each time.  After 40 minutes, we check again every 40 minutes without increasing the interval
     *
     * There is also a random fuzz factor that isn't tested here.
     */
    @Test
    fun testBackoff_GoesTo40MinutesThenStops() {
        setErrorCode(GLOBAL_FAIL_CODE)

        for (i in 1..config.maxBackoff) {
            val increment = config.backoffIncrement * i / 2

            // Halfway to the next timeout - should drop traffic
            testClock.time += increment
            checkIfDrop(DEFAULT_ENDPOINT, true)

            // All the way to the next timeout - should send request
            testClock.time += increment + 1
            checkIfDrop(DEFAULT_ENDPOINT, false)
            setErrorCode(GLOBAL_FAIL_CODE)
        }

        // 40 minutes later it should send the request again

        testClock.time += config.backoffIncrement *
                config.backoffIncrement + 1
        val shouldDrop = networkShutoffManager.shouldDropRequest(DEFAULT_ENDPOINT)
        assertEquals(false, shouldDrop)
    }

    @Test
    fun testGenerateErrorResponse_returnsValidResponseWithError() {
        val code = networkShutoffManager.getErrorCodeForResponse()
        assertEquals(556, code)
    }

    @Test
    fun testAfter556Code_blocksTrafficForOtherEndpoints() {
        setErrorCode(GLOBAL_FAIL_CODE, DEFAULT_ENDPOINT)
        checkIfDrop(ALTERNATE_ENDPOINT, true)
    }

    @Test
    fun testAfter556Code_generatesCorrectErrorCode() {
        setErrorCode(GLOBAL_FAIL_CODE, DEFAULT_ENDPOINT)
        val code = networkShutoffManager.getErrorCodeForResponse()
        assertEquals(ENDPOINT_FAIL_CODE, code)
    }

    @Test
    fun testAfterBlockingPeriodExpires_sendsAtLeastOneRequest() {
        setErrorCode(GLOBAL_FAIL_CODE, DEFAULT_ENDPOINT)
        setClockToNextInterval()
        checkIfDrop(ALTERNATE_ENDPOINT, false)
    }

    @Test
    fun testRestartAfterGlobalFailure() {
        setErrorCode(GLOBAL_FAIL_CODE)
        checkIfDrop(DEFAULT_ENDPOINT, true)
        checkIfDrop(ALTERNATE_ENDPOINT, true)
    }
}
