package com.yelp.android.varanus

/**
 * Interface for a persistent database that stores a log of all network requests so that if the
 * app is killed the state of the [EndpointSpecificNetworkTracker] can be restored.
 */
interface NetworkTrafficLogPersister {

    /**
     * Save the networkTrafficLog persistently.
     *
     * The database should retain all of this data, especially the endpoint key, as that will be
     * used to fetch the relevant data.
     */
    fun addLog(log: NetworkTrafficLog)

    /**
     * On startup, use this to fetch the amount of data and number of requests over the previous
     * time period.
     *
     * @param windowLength Time period we look back over to count up the number of relevant
     * requests.
     * @param endpoint Key used to assign data to the appropriate EndpointSpecificNetworkTracker.
     */
    fun getSizeAndClear(windowLength: Long, endpoint: String): TrafficLogSummary

    /**
     * Trim the size of our persistent database.
     *
     * @param windowLength Time period we look back over to count up the number of relevant
     * requests.
     * @param endpoint Key used to assign data to the appropriate EndpointSpecificNetworkTracker.
     */
    fun clear(windowLength: Long, endpoint: String)

    /**
     * Summary of the total number of requests and amount of data that has been sent over the
     * time period in question.
     */
    data class TrafficLogSummary(val count: Int, val size: Long)
}
