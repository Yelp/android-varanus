package com.yelp.varanussampleapp.persistence

import com.yelp.android.varanus.shutoff.NetworkShutoffLog
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Used by the EndpointSpecificShutoff to record the state of the endpoint, which decides whether
 * the endpoint should be blocked or not.
 */
open class RealmNetworkShutoffLog (
        // Realm requires a default empty constructor and so everything must have a default value
        var state: String = "SENDING",
        var backoffSize: Int = 1,
        var shutOffUntil: Long = 0,
        @PrimaryKey var endpoint: String = "stub"
) : RealmObject() {
    constructor(log: NetworkShutoffLog) : this(log.state,
            log.backoffSize,
            log.shutOffUntil,
            log.endpoint)
}
