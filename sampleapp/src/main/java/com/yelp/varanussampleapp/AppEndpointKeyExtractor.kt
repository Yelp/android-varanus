package com.yelp.varanussampleapp

import com.yelp.android.varanus.EndpointKeyExtractor
import okhttp3.Request

/**
 * Based on the network request that was sent, we determine the associate endpoint (or whatever
 * other useful category).  We keep track of statistics about that endpoint.
 */
class AppEndpointKeyExtractor: EndpointKeyExtractor {

    companion object {
        const val FOOD_LABEL = 3
        const val SERVICE = 2
    }

    override fun getEndpoint(request: Request) = request.url().encodedPathSegments()[FOOD_LABEL]

    override fun getType(request: Request) = request.url().encodedPathSegments()[SERVICE]
}

