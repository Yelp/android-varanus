package com.yelp.android.varanus

import okhttp3.Request

/**
 * Implement this to determine how to extract the endpoint name and endpoint type according to the
 * needs of your application.
 *
 * We use the term "Endpoint" because that is how we divide up our traffic, but you can really use
 * any arbitrary way of categorizing traffic (or always return the same thing if you only care
 * about traffic globally).
 */
interface EndpointKeyExtractor {
    /**
     * The key that you'll use throughout the network monitor for tracking traffic to an endpoint
     * or other category.
     */
    fun getEndpoint(request: Request): String

    /**
     * This can be used to make decisions about categories of endpoints.
     */
    fun getType(request: Request): String
}
