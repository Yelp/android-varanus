package com.yelp.android.varanus.shutoff

import org.junit.Test

/**
 * This tests specifically the functionality of blocking just one endpoint (as well as how that
 * interacts with other endpoints being blocked and global blocks.
 *
 * More general network traffic blocking functionality is tested in [GlobalNetworkShutoffManagerTest].
 */
class PerEndpointNetworkShutoffManagerTest : NetworkShutoffManagerTest() {
    @Test
    fun testReceive555_endpointIsShutOff() {
        setErrorCode(ENDPOINT_FAIL_CODE)
        checkIfDrop(DEFAULT_ENDPOINT, true)
    }

    @Test
    fun testRecieve555_otherEndpointsStillOn() {
        setErrorCode(ENDPOINT_FAIL_CODE)
        checkIfDrop(ALTERNATE_ENDPOINT, false)
    }

    @Test
    fun testAfter555CodeAndSuccessfulRetry_NoLongerDropsTraffic() {
        setErrorCode(ENDPOINT_FAIL_CODE)
        setClockToNextInterval()
        setErrorCode(SUCCESS_CODE)
        checkIfDrop(DEFAULT_ENDPOINT, false)
    }

    @Test
    fun testAfter555CodeAndFailedRetry_DropsTraffic() {
        setErrorCode(ENDPOINT_FAIL_CODE)
        setClockToNextInterval()
        setErrorCode(ENDPOINT_FAIL_CODE)
        checkIfDrop(DEFAULT_ENDPOINT, true)
    }

    @Test
    fun testTwoEndpointsBlockedThenOneCleared_blocksOnlyOneEndpoint() {
        setErrorCode(ENDPOINT_FAIL_CODE, DEFAULT_ENDPOINT)
        setErrorCode(ENDPOINT_FAIL_CODE, ALTERNATE_ENDPOINT)
        setErrorCode(SUCCESS_CODE, DEFAULT_ENDPOINT)
        checkIfDrop(DEFAULT_ENDPOINT, false)
        checkIfDrop(ALTERNATE_ENDPOINT, true)
    }

    @Test
    fun testEndpointAndGlobalBlocked_clearingGlobalDoesntClearEndpoint() {
        // We set both to block traffic
        setErrorCode(ENDPOINT_FAIL_CODE, DEFAULT_ENDPOINT)
        setErrorCode(GLOBAL_FAIL_CODE, DEFAULT_ENDPOINT)

        // Once we try again, a different endpoint gets through
        setErrorCode(SUCCESS_CODE, ALTERNATE_ENDPOINT)

        // The global block should be gone but the endpoint-specific one is not clear
        checkIfDrop(DEFAULT_ENDPOINT, true)
        checkIfDrop(ALTERNATE_ENDPOINT, false)
    }

    @Test
    fun testEndpointBlockCode_doesntClearGlobalBlock() {
        // Enable the global block
        setErrorCode(GLOBAL_FAIL_CODE)

        // Once we try again, we get an endpoint-specific block
        setClockToNextInterval()
        setErrorCode(ENDPOINT_FAIL_CODE)

        // Once we try again, the global block should still be in effect
        checkIfDrop(ALTERNATE_ENDPOINT, false)
    }

    @Test
    fun testEndpointAndGlobalBlocked_enablingGlobalDoesntClearEndpoint() {
        setErrorCode(ENDPOINT_FAIL_CODE)
        setErrorCode(GLOBAL_FAIL_CODE)
        checkIfDrop(DEFAULT_ENDPOINT, true)
    }

    @Test
    fun testRestartAfterEndPointFailure() {
        setErrorCode(ENDPOINT_FAIL_CODE)
        checkIfDrop(DEFAULT_ENDPOINT, true)
    }
}
