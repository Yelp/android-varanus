package com.yelp.android.varanus.okhttp

import com.yelp.android.varanus.EndpointKeyExtractor
import com.yelp.android.varanus.NetworkMonitor
import com.yelp.android.varanus.NetworkTrafficLog
import com.yelp.android.varanus.shutoff.NetworkShutoffManager
import com.yelp.android.varanus.util.CoroutineScopeAndJob
import com.yelp.android.varanus.util.JobBasedScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.Request

/**
 * An Interceptor that logs the amount of data sent in each request over OkHttp.
 *
 * This should be set up as a networkInterceptor, in order to accurately measure the number of
 * bytes sent over the wire.
 *
 * When using OkHttp, this is the originating point of all Varanus related functionality.
 *
 * @param monitor The network monitor to which logs are submitted
 * @param endpointKeyExtractor Given a request, extracts a label (e.g. name of endpoint) to
 * categorize the request with.
 * @param networkShutoffManager Class responsible for shutting off traffic on receipt of a signal.
 */
class TrafficMonitorInterceptor(
        val monitor: NetworkMonitor,
        private val endpointKeyExtractor: EndpointKeyExtractor,
        private val networkShutoffManager: NetworkShutoffManager
) : Interceptor,
        CoroutineScopeAndJob by JobBasedScope(Dispatchers.IO) {

    override fun intercept(chain: Chain): Response {
        val request = chain.request()
        val endpoint = endpointKeyExtractor.getEndpoint(request)
        val endpointType = endpointKeyExtractor.getType(request)

        val length = request.body()?.contentLength() ?: 0

        launch {
            monitor.addLog(NetworkTrafficLog(true, endpoint, endpointType, length))
        }

        if (networkShutoffManager.shouldDropRequest(endpoint)) {
            return generateErrorResponse(request, networkShutoffManager)
        }

        val response = chain.proceed(request)

        // This is where the network shutoff man
        networkShutoffManager.determineShutoffStatusFromRequest(response, endpoint)

        val body: ResponseBody? = response.body()

        // This allows Varanus to count up the volume of traffic being received over time.
        return if (body != null) {
            response.newBuilder()
                    .body(ProgressResponseBody(body,
                            RequestProgressListener(monitor, endpoint, endpointType)))
                    .build()
        } else {
            response
        }
    }

    /**
     * When we're blocking traffic, we substitute responses with this fake response.  It's up to
     * the relevant developer to make sure that the app behaves in the desired way in response to
     * these errors.
     *
     * It will return the appropriate error code that was used to indicate traffic
     * should be blocked. A global block takes precedence.
     *
     * @param request The actual network request that's being blocked.
     * @param errorCode The error code that sent the signal to block this request.
     */
    private fun generateErrorResponse(request: Request,
                                      networkShutoffManager: NetworkShutoffManager
    ): Response {
        val errorCode = networkShutoffManager.getErrorCodeForResponse()
        val body = ResponseBody.create(MediaType.get("text/plain"), "")
        return Response.Builder()
                .code(errorCode)
                .request(request)
                .body(body)
                .message("Server overloaded. Request dropped due to error code $errorCode.")
                .protocol(Protocol.HTTP_2)
                .build()
    }

    /**
     * Keeps track of how much data has been sent in a response and adds a log to the network
     * monitor when a request is completed.  Used by ProgressResponseBody.
     *
     * @param monitor The network monitor to which logs are submitted
     * @param endpoint For submitting the log, the name of the endpoint or other category used for
     * tracking statistics about this request.
     * @param endpointType If you want to track broader categories of endpoints as well.
     */
    inner class RequestProgressListener(
        val monitor: NetworkMonitor,
        val endpoint: String,
        private val endpointType: String
    ) : ProgressResponseBody.ProgressListener {

        private var totalBytesRead = 0L
        override fun update(bytesRead: Long) {
            totalBytesRead += bytesRead
        }

        /**
         * Adds the bytes read so far, built up by this listener, to a log that is added to
         * the network monitor.
         */
        override fun done() {
            launch {
                monitor.addLog(
                        NetworkTrafficLog(false, endpoint, endpointType, totalBytesRead))
            }
        }
    }
}
