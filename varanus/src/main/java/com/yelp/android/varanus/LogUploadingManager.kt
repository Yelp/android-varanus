package com.yelp.android.varanus

import com.google.android.gms.common.util.Clock
import com.google.android.gms.common.util.DefaultClock

const val TOTAL = "total"


/**
 * This class determines whether or not to send logs somewhere, and then does so.  It does so with
 * the help of whatever extends [LogUploaderBase], which contains the app-specific part of the
 * functionality.
 *
 * It applies to all endpoints - all alerts are stored in the [LogUploaderBase], , and when there is
 * network traffic, all alerts across all endpoints are flushed, no more than every
 * [maxSendFrequency] minutes.
 *
 * @param logUploader Contains code to issue an alert in an app-appropriate way.
 * @param windowLength Length of time to save network logs that we might send. Should be equal to or
 * longer than maxSendFrequency.
 * @param maxSendFrequency Max frequency with which logs might be sent.
 */
class LogUploadingManager(
        private val logUploader: LogUploaderBase,
        private val windowLength: Long,
        private val maxSendFrequency: Long
) {
    private var lastTimeCleared = -maxSendFrequency
    private var clock: Clock = DefaultClock.getInstance()

    internal fun setClockForTesting(clock: Clock) {
        this.clock = clock
    }

    /**
     * This takes the statistics about data sent to each endpoint, and using the [logUploader]
     * you have defined, uploads summary statistics about each endpoint and overall.
     *
     * It also clears the state of the network log tracking for each endpoint so you don't
     * double-count.
     */
    suspend fun registerLogs(
            endpoints: HashMap<String, EndpointSpecificNetworkTracker>
    ) {
        if (clock.elapsedRealtime() - lastTimeCleared < maxSendFrequency) return
        lastTimeCleared = clock.elapsedRealtime()

        var size = 0L
        var count = 0

        endpoints.forEach {(endpoint, tracker) ->

            if (tracker.requestCount.get() != 0) {

                // These must happen first because the endpoint gets cleared
                size += tracker.requestSize.get()
                count += tracker.requestCount.get()

                // Then upload the log for this endpoint
                logUploader.uploadTrafficLogSummaryForInterval(tracker.requestSize.get(),
                        tracker.requestCount.get(),
                        windowLength,
                        endpoint)
                tracker.clearLog()
            }
        }

        logUploader.uploadTrafficLogSummaryForInterval(size, count, windowLength, TOTAL)
        endpoints[TOTAL]?.clearLog()

    }

    /**
     * Extend this with a class that uploads logs to the appropriate place.
     */
    interface LogUploaderBase {

        suspend fun uploadTrafficLogSummaryForInterval(
                data: Long,
                requests: Int,
                interval: Long,
                endpoint: String
        )
    }
}
