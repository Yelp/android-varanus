package com.yelp.varanussampleapp.persistence

import com.yelp.android.varanus.shutoff.NetworkShutoffLogPersister

import com.yelp.android.varanus.shutoff.NetworkShutoffLog
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.kotlin.where
import java.util.LinkedList

/**
 * Used by EndpointSpecificShutoff to save its state.
 *
 * Logs should be added from here but only read when state needs to be restored after the app
 * starts up again.
 *
 * @param persistentCacheRepository Repository containing an instance of the realm configuration
 * we use for this app.
 */
class AppNetworkShutoffLogPersister(
        private val realmConfig: RealmConfiguration
) : NetworkShutoffLogPersister {

    /**
     * Used by NetworkShutoffLogManager to restore its state
     *
     * @param endpoints A container used to hold all endpoints names.
     */
    override fun getAll(): LinkedList<String> {
        val endpoints = LinkedList<String>()
        Realm.getInstance(realmConfig).use { realm ->
            realm.where<RealmNetworkShutoffLog>().findAll().mapTo(endpoints) { it.endpoint }
        }
        return endpoints
    }

    /**
     * Delete the data entry whose key is the endpoint string
     *
     * @param endpoint Key for the endpoint type we're deleting data for
     */
    override fun clear(endpoint: String) {
        Realm.getInstance(realmConfig).use { realm ->
            realm.where<RealmNetworkShutoffLog>()
                    .equalTo("endpoint", endpoint)
                    .findAll()
                    .deleteAllFromRealm()
        }
    }

    /**
     * @param endpoint The key used to store this network request
     * @return the state to the log whose key is the endpoint if it exists in the realm. Otherwise,
     * the log in the default state is returned.
     */
    override fun getLog(endpoint: String): NetworkShutoffLog {
        // Fetch the log matching the entrypoint. There should be at most one log exists since
        // entrypoint is the primary key
        var state = "SENDING"
        var backoffSize = 1
        var shutOffUntil = 0L

        Realm.getInstance(realmConfig).use { realm ->
            val log: RealmNetworkShutoffLog? =
                    realm.where<RealmNetworkShutoffLog>()
                            .equalTo("endpoint", endpoint)
                            .findFirst()

            // if there is a log, reset the state and remove the log
            state = log?.state ?: "SENDING"
            backoffSize = log?.backoffSize ?: 1
            shutOffUntil = log?.shutOffUntil ?: 0L
        }

        return NetworkShutoffLog(state, backoffSize, shutOffUntil, endpoint)
    }

    /**
     * Save a networkshutoff log to realm
     *
     * @param log The log to save to Realm.
     */
    override fun addAndUpdateLog(log: NetworkShutoffLog) {
        Realm.getInstance(realmConfig).use { realm ->
            realm.executeTransaction {
                it.insertOrUpdate(RealmNetworkShutoffLog(log))
            }
        }
    }
}
