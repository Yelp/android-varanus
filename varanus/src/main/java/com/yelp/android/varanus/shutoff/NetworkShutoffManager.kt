package com.yelp.android.varanus.shutoff

import com.google.android.gms.common.util.Clock
import com.google.android.gms.common.util.DefaultClock
import com.yelp.android.varanus.util.CoroutineScopeAndJob
import com.yelp.android.varanus.util.JobBasedScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToLong

const val GLOBAL = "global"
/**
 * Manages global state for what traffic to shut off.
 *
 * Most functionality is delegated to [CategoryOfTrafficShutoff] and this is primarily responsible
 * for delegating to the correct [CategoryOfTrafficShutoff].
 *
 * @param clock For determining shutoff timeouts.
 * @param randomizer For randomizing how long the shutoff happens to stop everyone from retyring at
 * once.
 * @param persister Save the state so that if the app is crashing while sending too much data it
 * doesn't keep sending more data.
 * @param config Sets backoff times, etc.
 */
class NetworkShutoffManager @JvmOverloads constructor(
    private val clock: Clock = DefaultClock.getInstance(),
    private val randomizer: Randomizer,
    private val persister: NetworkShutoffLogPersister,
    private val shutoffConfig: Config
) : CoroutineScopeAndJob by JobBasedScope(Dispatchers.IO) {

    private val globalShutoff by lazy {
        CategoryOfTrafficShutoff(clock, randomizer, persister, GLOBAL, shutoffConfig)
    }
    private val endpointShutoffs = ConcurrentHashMap<String, CategoryOfTrafficShutoff>()

    init {
        launch {
            persister.getAll().filter { it != GLOBAL }.forEach { endpoint ->
                endpointShutoffs[endpoint] =
                        CategoryOfTrafficShutoff(
                                clock,
                                randomizer,
                                persister,
                                endpoint,
                                shutoffConfig)
            }
        }
    }

    /**
     * Look out for error codes that trigger blocking traffic and set blocks accordingly.
     *
     * @param response The response from the network
     * @param endpoint A string that identified the related endpoint, used by per-endpoint blocking
     */
    @Synchronized
    fun determineShutoffStatusFromRequest(response: Response?, endpoint: String) {
        when (response?.code) {
            shutoffConfig.globalShutoffCode -> globalShutoff.shutoff()
            shutoffConfig.endpointShutoffCode -> setEndpointSpecificShutoff(endpoint)
            else -> resetEndpoint(endpoint)
        }
    }

    /**
     * Helper function to set up the CategoryOfTrafficShutoff if necessary, before calling
     * shutoff() on it
     */
    private fun setEndpointSpecificShutoff(endpoint: String) {
        var endpointManager = endpointShutoffs[endpoint]
        if (endpointManager == null) {
            endpointManager = CategoryOfTrafficShutoff(
                    clock,
                    randomizer,
                    persister,
                    endpoint,
                    shutoffConfig)
            endpointManager = endpointShutoffs.putIfAbsent(endpoint, endpointManager)
                    ?: endpointManager
        }
        endpointManager.shutoff()
    }

    /**
     * Helper function to inform relevant endpoints that a signal has been received that traffic can
     * now be sent.  A successful request resets any global block on traffic, as well as blocks on
     * that specific endpoint.
     */
    private fun resetEndpoint(endpoint: String) {
        globalShutoff.reset()
        endpointShutoffs[endpoint]?.reset()
    }

    /**
     * Whether or not this type of traffic is being blocked.
     */

    fun shouldDropRequest(endpoint: String): Boolean {
        val shouldDropForEndpoint = endpointShutoffs[endpoint]?.shouldDropRequest() ?: false
        return when (shouldDropForEndpoint) {
            true -> true
            false -> globalShutoff.shouldDropRequest()
        }
    }

    /**
     * Determine what error code is causing network requests to be blocked.
     * Global blocks take precedence over endpoint-specific blocks.
     */
    fun getErrorCodeForResponse(): Int {
        return when (globalShutoff.shouldDropRequest()) {
            true -> shutoffConfig.endpointShutoffCode
            false -> shutoffConfig.globalShutoffCode
        }

    }

    /**
     * This wraps Math.random so that it can be mocked in tests.
     */
    open class Randomizer(private val shutoffConfig: Config) {
        open fun randomizeTime(time: Long) =
                time + (Math.random() * shutoffConfig.backoffSpread).roundToLong()
        open fun randomizeSendRequest(oneOutOf: Int) = (Math.random() * oneOutOf) < 1
    }

    /**
     * @param backoffIncrement How many minutes to back off by each time.
     * @param maxBackoff The maximum number of minutes to wait when backing off
     * @param attemptingThrottle Drop 1 out of X requests when trying again to prevent overload.
     * @param globalShutoffCode HTTP error code to signal shut off all traffic.
     * @param endpointShutoffCode HTTP error code to signal shut off traffic associated with this
     * endpoint only.
     * @param backoffSpread What length of time to spread the randomization of the backoff over
     * (minutes)
     */
    data class Config(
            val backoffIncrement: Long,
            val maxBackoff: Int,
            val attemptingThrottle: Int,
            val globalShutoffCode: Int,
            val endpointShutoffCode: Int,
            val backoffSpread: Long)
}
