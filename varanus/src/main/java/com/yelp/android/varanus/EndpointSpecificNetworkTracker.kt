package com.yelp.android.varanus

import com.yelp.android.varanus.util.CoroutineScopeAndJob
import com.yelp.android.varanus.util.JobBasedScope
import kotlinx.coroutines.Dispatchers

/**
 * This class keeps track of network usage statistics in a given category. Usually by endpoint,
 * but can be used to keep track of any arbitrary category of traffic of interest.
 *
 * In endpointConfig there is a string called "endpoint".  This is a key that is used to look
 * up this [EndpointSpecificNetworkTracker].  The key can correspond to anything (although usually
 * to and endpoint) as long as the desired set of network requests are always mapped to that key.
 *
 * The way this works is that each request is stored in a cache, which expires data after a length
 * of time.  In this way, the requests in the cache represent the last windowLength minutes of
 * requests.  Requests are also stored in a persistent database so this tracking can remain
 * approximatly accurate even if the app is restarted repeatedly in quick succession.
 *
 * @param endpoint Label for this endpoint.
 * @param windowLength Time in milliseconds for the window of time for which we observe requests.
 * @param trafficLogPersister Uses a persistent database to make sure state about the total number
 * of requests is retained.
 * @param networkTrafficAlerter Sends alerts to the desired place according to how the app sends
 * alerts or logs.
 */
class EndpointSpecificNetworkTracker(
        private val endpoint: String,
        private val windowLength: Long,
        private val trafficLogPersister: NetworkTrafficLogPersister,
        private val networkTrafficAlerter: LogUploadingManager
) : CoroutineScopeAndJob by JobBasedScope(Dispatchers.IO) {

    var requestCount: Int = 0
    var requestSize: Long = 0L


    /**
     * We've been saving all the logs into a persistent database, so we can restore our
     * understanding of the number of recent requests when initialized.
     */
    init {
        val priorValues = trafficLogPersister.getSizeAndClear(windowLength, endpoint)
        requestCount = priorValues.count
        requestSize = priorValues.size

        // This fake network log makes sure the previous request count and request size expires.
        // Otherwise we could get into a state where we keep incrementing these.
        val initialTrafficLog = NetworkTrafficLog(true, endpoint, "n/a",
                requestSize, requestCount)

    }

    /**
     * Keep track of the number and size of requests for a specific endpoint.
     *
     * @param log abstraction of a network request with the size and endpoint key.
     */
    @Synchronized
    fun addLogAndPersist(log: NetworkTrafficLog) {
        if (log.isRequest) {
            requestCount++
        }
        requestSize += log.size
        trafficLogPersister.addLog(log)
    }

    /**
     * Once we've logged these requests, we delete them so we don't double-count.
     */
    @Synchronized
    fun clearLog() {
        requestCount = 0
        requestSize = 0
        trafficLogPersister.clear(windowLength, endpoint)
    }
}
