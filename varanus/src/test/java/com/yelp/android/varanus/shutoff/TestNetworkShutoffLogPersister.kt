package com.yelp.android.varanus.shutoff

/**
 * Used to to test network shutoff.
 */
class TestNetworkShutoffLogPersister : NetworkShutoffLogPersister {

    private val logs = HashMap<String, NetworkShutoffLog>()

    override fun getAll(): List<String> {

        return logs.keys.toList()
    }

    /**
     * Delete the entry whose key is the entrypoint.
     *
     * @param endpoint Key for the endpoint type we're deleting data for.
     */
    override fun clear(endpoint: String) {
    }

    /**
     * @param endpoint The key used to store this network request.
     * @return the log whos key is the endpoint.
     */
    override fun getLog(endpoint: String): NetworkShutoffLog {
        return logs[endpoint] ?: NetworkShutoffLog("SENDING", 1, 0L, endpoint)
    }

    /**
     * Save a log of [EndpointSpecificShutoff] to realm.
     *
     * @param log The log to save to Realm.
     */
    override fun addAndUpdateLog(log: NetworkShutoffLog) {
        logs[log.endpoint] = log
    }
}
