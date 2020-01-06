package com.yelp.android.varanus.shutoff

/**
 * Interface for a persistent database that stores a log of all [EndpointSpecificShutoff] states so
 * that if the app is killed the state of the [EndpointSpecificShutoff] can be restored.
 */
interface NetworkShutoffLogPersister {

    fun addAndUpdateLog(log: NetworkShutoffLog)

    fun getLog(endpoint: String): NetworkShutoffLog

    fun getAll(): List<String>

    fun clear(endpoint: String)
}
