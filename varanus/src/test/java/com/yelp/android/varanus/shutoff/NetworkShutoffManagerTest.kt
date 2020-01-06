package com.yelp.android.varanus.shutoff

import com.yelp.android.varanus.TestClock
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Before
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

/**
 * Common code between [GlobalNetworkShutoffManagerTest] and [PerEndpointNetworkShutoffManagerTest].
 */
abstract class NetworkShutoffManagerTest {

    companion object {
        internal const val GLOBAL_FAIL_CODE = 556
        internal const val ENDPOINT_FAIL_CODE = 555
        internal const val SUCCESS_CODE = 200
        internal const val DEFAULT_ENDPOINT = "test"
        internal const val ALTERNATE_ENDPOINT = "test2"
    }

    internal val defaultRequest = Request.Builder().url("https://www.yelp.com").build()
    internal lateinit var testClock: TestClock
    internal lateinit var networkShutoffManager: NetworkShutoffManager

    private val config = TestConfig.config

    @Before
    fun setUp() {
        testClock = TestClock()
        networkShutoffManager =
                NetworkShutoffManager(testClock,
                        TestRandomizer(config),
                        TestNetworkShutoffLogPersister(),
                        config)
    }

    internal fun setErrorCode(code: Int, endpoint: String = DEFAULT_ENDPOINT) {
        val response = Response.Builder().request(defaultRequest).message("test")
                .protocol(Protocol.HTTP_2).code(code).build()
        networkShutoffManager.determineShutoffStatusFromRequest(response, endpoint)
    }

    internal fun checkIfDrop(endpoint: String, expected: Boolean) {
        val shouldDrop = networkShutoffManager.shouldDropRequest(endpoint)
        assertEquals(expected, shouldDrop)
    }

    internal fun setClockToNextInterval() {
        testClock.time += config.backoffIncrement + 1
    }

    class TestRandomizer(shutoffConfig: NetworkShutoffManager.Config
    ) : NetworkShutoffManager.Randomizer(shutoffConfig) {
        override fun randomizeTime(time: Long) = time
    }
}
