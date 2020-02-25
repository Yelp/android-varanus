package com.yelp.android.varanus

import com.yelp.android.varanus.LogUploadingManager.LogUploaderBase
import kotlin.collections.HashMap

/**
 * Manages keeping track of network traffic trends using many different
 * [EndpointSpecificNetworkTracker]s to keep track of different categories of traffic.
 *
 * The actual work of tracking traffic is done by the [EndpointSpecificNetworkTracker]s, which use
 * the [LogUploaderBase] to send alerts if needed.  This class serves mainly to manage the
 * [EndpointSpecificNetworkTracker]s and delegate logging to the appropriate one accordingly.
 *
 * @param windowLength Our window in time for
 * @param persister Interacts with the app's mechanism of persisting data to ensure
 * that state is maintained between app start-ups.
 * @param alertIssuer Interacts with the app's method of sending alerts or logs to a server.
 */
class NetworkMonitor(
        private val windowLength: Long,
        clear_increment: Long,
    private val persister: NetworkTrafficLogPersister,
    alertIssuer: LogUploaderBase
) {
    private val networkTrafficAlerter =
            LogUploadingManager(alertIssuer, windowLength, clear_increment)
    private var endpoints = HashMap<String, EndpointSpecificNetworkTracker>()


    /**
     * Every time there's a request, increment the size and count of the corresponding endpoint
     * as well as the tracker that tracks the total across all endpoints.
     *
     * Creates a new endpoint-specific tracker if needed, and calls that tracker to add a log
     * and check if an alert needs to be sent.
     *
     * @param log Abstraction of a network request with the size and an endpoint key.
     */
    suspend fun addLog(log: NetworkTrafficLog) {
        if (log.endpoint !in endpoints.keys) {

            val endpointTracker = EndpointSpecificNetworkTracker(
                    log.endpoint,
                    windowLength,
                    persister,
                    networkTrafficAlerter)

            endpoints[log.endpoint] = endpointTracker
        }

        /* We might lose logs if things aren't initialized yet but in any reasonable scenario  one
        would worry about, the problem will persist long enough to send logs later.
        We decided that the small risk of an extremely unusual problem going unnoticed is
        outweighed by the performance hit of initializing the network monitor synchronously. */
        endpoints[log.endpoint]?.addLogAndPersist(log)
        networkTrafficAlerter.registerLogs(endpoints)
    }
}
