package com.yelp.varanussampleapp.persistence

import com.yelp.android.varanus.NetworkTrafficLog
import com.yelp.android.varanus.NetworkTrafficLogPersister
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults

class AppNetworkTrafficLogPersister(
    private val realmConfig: RealmConfiguration
) : NetworkTrafficLogPersister {

    /**
     * Delete all data that is from before the specified time window
     *
     * @param windowLength Window before now for which we should retain data
     * @param endpoint Key for the endpoint type we're deleting data for
     */
    override fun clear(windowLength: Long, endpoint: String) {
        val earliestTime = System.currentTimeMillis() - windowLength
        Realm.getInstance(realmConfig).use { realm ->
            realm.executeTransaction {
                it.where(RealmNetworkLog::class.java)
                        .lessThan("time", earliestTime)
                        .findAll()
                        .deleteAllFromRealm()
            }
        }
    }

    /**
     * Determine the number of requests and amount of data sent over the last time window.
     * Then, delete all data that falls outside that window.
     *
     * @param windowLength The amount of time in the past for which to retain log data
     * @param endpoint The key used to store this network request
     */
    override fun getSizeAndClear(
            windowLength: Long,
            endpoint: String
    ): NetworkTrafficLogPersister.TrafficLogSummary {

        var count = 0
        var size = 0L
        Realm.getInstance(realmConfig).use {  realm ->
            val earliestTime = System.currentTimeMillis() - windowLength
            val logs: RealmResults<RealmNetworkLog> =
                    realm.where(RealmNetworkLog::class.java)
                            .equalTo("endpoint", endpoint)
                            .greaterThan("time", earliestTime)
                            .findAll()
            count = logs.size
            size = logs.sum("size").toLong()
        }

        clear(windowLength, endpoint)
        return NetworkTrafficLogPersister.TrafficLogSummary(count, size)
    }

    /**
     * Save a log about a network request to Realm.
     *
     * @param log The log to save to Realm.
     */
    override fun addLog(log: NetworkTrafficLog) {
        Realm.getInstance(realmConfig).use {  realm ->
            realm.executeTransaction {
                it.insertOrUpdate(RealmNetworkLog(log))
            }
        }
    }
}