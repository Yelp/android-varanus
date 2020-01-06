package com.yelp.android.varanus.shutoff

/**
 * The state of [CategoryOfTrafficShutoff] , containing all the information needed to decide
 * whether or not to block an endpoint.
 *
 * @param state The state of CategoryOfTrafficShutoff, which could be Inactive, Shutoff,
 * or Attempting.
 * @param backoffSize How much we've been backing off.
 * @param shutOffUntil How long we've backed off until.
 * @param endpoint The key used to decide which EndpointSpecificShutoff to use.
 */
data class NetworkShutoffLog(
    var state: String,
    var backoffSize: Int,
    var shutOffUntil: Long,
    var endpoint: String
)
