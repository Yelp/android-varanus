package com.yelp.varanussampleapp.persistence

import com.yelp.android.varanus.NetworkTrafficLog
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.UUID

/**
 * Used by the traffic monitor to record that an amount of network traffic has been sent at a
 * particular time.
 *
 * In general, there will be one entry per request. However, we delete these quite aggressively.
 */
open class RealmNetworkLog(
        // Realm requires a default empty constructor and so everything must have a default value
        var isRequest: Boolean = false,
        var endpoint: String? = null,
        var size: Long = 0,
        var time: Long = 0,
        @PrimaryKey protected var id: String = UUID.randomUUID().toString()
) : RealmObject() {

    constructor(log: NetworkTrafficLog) : this(log.isRequest,
            log.endpoint,
            log.size,
            log.date.time)
}
