package com.yelp.android.varanus.okhttp

import okhttp3.ResponseBody
import okio.Buffer	import okio.*
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source

/**
 * This is used by [TrafficMonitorInterceptor] to keep track of statistics on the network responses
 * received as a result of requests sent.
 */
class ProgressResponseBody(
    private val responseBody: ResponseBody,
    progressListener: ProgressListener
) : ResponseBody() {

    private val bufferedSource: BufferedSource =
            ProgressTrackingSource(responseBody.source(), progressListener).buffer()

    override fun contentType() = responseBody.contentType()

    override fun source() = bufferedSource

    override fun contentLength() = responseBody.contentLength()

    /**
     * This should be extended by something which records and then logs (or otherwise acts on)
     * the size of the response.
     */
    interface ProgressListener {
        /**
         * Called whenever there's more data in a request.
         *
         * @param bytesRead the amount of data in this batch (not the entire response).
         */
        fun update(bytesRead: Long)

        /**
         * Called when all data has been received.
         */
        fun done()
    }

    /**
     * Helper class for tracking the response size.
     *
     * @param delegate the [ResponseBody] source, needed by the superclass.
     * @param progressListener class which is updated with the progress of how much has been
     * downloaded.
     */
    class ProgressTrackingSource(delegate: Source, private val progressListener: ProgressListener) :
            ForwardingSource(delegate) {

        override fun read(sink: Buffer, byteCount: Long): Long {
            val bytesRead = super.read(sink, byteCount)
            if (bytesRead == -1L) {
                progressListener.done()
            } else {
                progressListener.update(bytesRead)
            }
            return bytesRead
        }
    }
}
