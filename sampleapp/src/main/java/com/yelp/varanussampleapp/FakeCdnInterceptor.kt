package com.yelp.varanussampleapp

import com.yelp.android.varanus.EndpointKeyExtractor
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.Protocol
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType

private const val SPECIFIC_ENDPOINT_ERROR_CODE = 555
private const val ALL_ENDPOINT_ERROR_CODE = 556

// Arbitrarily chosen numbers for demonstration purposes
private const val TOO_MANY_REQUESTS_FROM_ONE_ENDPOINT = 10
private const val TOO_MANY_REQUESTS_FROM_ALL_ENDPOINTS = 30

/**
 * This simulates the functionality of a CDN which you can configure to turn off traffic.
 *
 * In a real implementation, this should a) be a real CDN and b) be something that you do manually.
 * If you don't have a CDN, you would do this in the backend somehow.  This is something you want
 * to talk to your Production Engineering/Operations/etc team about.
 *
 * (https://en.wikipedia.org/wiki/Content_delivery_network)
 */
class FakeCdnInterceptor(
        private val endpointKeyExtractor: EndpointKeyExtractor
) : Interceptor {

    private val requestCount = mutableMapOf<String, Int>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val endpoint = endpointKeyExtractor.getEndpoint(request)
        requestCount[endpoint] = (requestCount[endpoint] ?: 0) + 1

        if (requestCount[endpoint]?: 0 > TOO_MANY_REQUESTS_FROM_ONE_ENDPOINT) {
            return makeErrorResponse(request, SPECIFIC_ENDPOINT_ERROR_CODE)
        }

        if (requestCount.map{it.value}.sum() > TOO_MANY_REQUESTS_FROM_ALL_ENDPOINTS) {
            return makeErrorResponse(request, ALL_ENDPOINT_ERROR_CODE)
        }

        return chain.proceed(request)
    }

    private fun makeErrorResponse(request: Request, code: Int): Response {
        return Response.Builder()
                .code(code)
                .request(request)
                .message("oh no") // not used, but okhttp expects this to be non-null
                .body(ResponseBody.create("text/plain".toMediaType(), "oh no")) // not used
                .protocol(Protocol.HTTP_2)
                .build()
    }
}
