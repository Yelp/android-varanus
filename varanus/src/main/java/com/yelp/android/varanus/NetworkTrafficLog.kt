package com.yelp.android.varanus

import java.util.Date

/**
 * A summary of a network request or response, containing all the information needed to decide
 * whether or not to send an alert.
 *
 * @param isRequest True means a request, false means a response. We may treat these differently,
 * e.g. counting only the number of requests but not the number of responses to avoid
 * double-counting.
 * @param endpoint The key used to decide which [EndpointSpecificNetworkTracker] to use.
 * @param endpointType An optional additional label for further categorizing endpoints.
 * @param size Total number of bytes sent over the wire, or as close as we can measure.
 * @param count Total number of requests.
 * @date date The time, at least approximately, that it was sent.
 */
data class NetworkTrafficLog(
    var isRequest: Boolean,
    var endpoint: String,
    var endpointType: String,
    var size: Long,
    var count: Int = 1,
    var date: Date = Date())
