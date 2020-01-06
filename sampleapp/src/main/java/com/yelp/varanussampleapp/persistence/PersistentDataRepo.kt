package com.yelp.varanussampleapp.persistence

import android.content.Context
import io.realm.Realm
import io.realm.RealmConfiguration

class PersistentDataRepo(val context: Context) {
    companion object {
        const val REALM_SCHEMA_VERSION = 0L

        val persistentRepo : PersistentDataRepo? = null

        fun getPersistentRepo(context: Context) : PersistentDataRepo {
            return persistentRepo ?: PersistentDataRepo(context)

        }
    }
    val realmConfig: RealmConfiguration
    val networkShutoffLogPersister : AppNetworkShutoffLogPersister
    val networkTrafficLogPersister : AppNetworkTrafficLogPersister

    init {
        // Make sure to add a migration and associated tests if this is ever updated
        Realm.init(context)
        realmConfig = RealmConfiguration.Builder().schemaVersion(REALM_SCHEMA_VERSION).build()
        networkShutoffLogPersister = AppNetworkShutoffLogPersister(realmConfig)
        networkTrafficLogPersister = AppNetworkTrafficLogPersister(realmConfig)

    }
}