package com.yelp.android.varanus.shutoff

import com.google.android.gms.common.util.Clock
import com.google.android.gms.common.util.DefaultClock

/**
 * This manages a category of shutoffs for a batch of traffic that we want to treat as a unit
 * for the purpose of shutting it off. Usually, this category of traffic corresponds to an endpoint
 * but all of the traffic is one such category.
 *
 * @param clock For determining shutoff timeouts.
 * @param randomizer For randomizing how long the shutoff happens to stop everyone from retyring at
 * once.
 * @param persister Save the state so that if the app is crashing while sending too much data it
 * doesn't keep sending more data.
 * @param endpoint Label for the type of traffic this object handles.
 * @param config Sets backoff times, etc.
 */
class CategoryOfTrafficShutoff(
    private val clock: Clock = DefaultClock.getInstance(),
    private val randomizer: NetworkShutoffManager.Randomizer,
    private val persister: NetworkShutoffLogPersister,
    endpoint: String,
    private val config: NetworkShutoffManager.Config
) {

    enum class State {
        SENDING, // this class acts as a no-op, send requests full speed
        SHUTOFF, // this class blocks all requests
        ATTEMPTING // send 1 request then throttle to see if it's safe to send full speed again
    }

    private var shutOffState = State.SENDING
    private var shutOffUntil = 0L
    private var backoffSize = 1

    // For persisting this state in case of crashes
    private var shutOffLog =
            NetworkShutoffLog(shutOffState.toString(), backoffSize, shutOffUntil, endpoint)

    /**
     * We start by restoring any old saved state.
     */
    init {
        val priorValues = persister.getLog(endpoint)
        shutOffState = when (priorValues.state) {
            // We renamed this state for clarity - this is for backwards compatiblity with devices
            // with old values saved persistently
            "INACTIVE" -> State.SENDING
            else -> State.valueOf(priorValues.state)
        }
        backoffSize = priorValues.backoffSize
        shutOffUntil = priorValues.shutOffUntil
        // Update shutoffLog
        shutOffLog =
                NetworkShutoffLog(shutOffState.toString(), backoffSize, shutOffUntil, endpoint)
    }

    /**
     * We have successfully sent a request without being blocked.
     *
     * We then switch to sending, and reset the shutoff time and the backoff increment.
     */
    fun reset() {
        if (shutOffState == State.SENDING) { return }
        shutOffState = State.SENDING
        shutOffUntil = 0L
        backoffSize = 1

        saveState()
    }

    /**
     * A signal has been received that this category should be  shut off, or that the shutoff
     * should be renewed.
     *
     * Each time this is called, until reset() is called, we wait for longer, up to the limit
     * set by config.maxBackoff.
     */
    fun shutoff() {
        shutOffState = State.SHUTOFF
        shutOffUntil = clock.elapsedRealtime() + backoffSize * config.backoffIncrement

        // Make sure all the devices don't retry at the same time and knock the server over
        shutOffUntil = randomizer.randomizeTime(shutOffUntil)
        backoffSize = (backoffSize + 1).coerceAtMost(config.maxBackoff)

        saveState()
    }

    /**
     * A helper function which saves the current state and backoffSize of this endpoint to realm.
     * Should be called after every state change.
     */
    private fun saveState() {
        shutOffLog.state = shutOffState.toString()
        shutOffLog.backoffSize = backoffSize
        shutOffLog.shutOffUntil = shutOffUntil
        persister.addAndUpdateLog(shutOffLog)
    }

    /**
     * For this particular endpoint, determine if requests should be sent.
     *
     * This also triggers checking if the shutoff has expired and updating the state accordingly.
     * If this happens, exactly one request (this request) will be allowed through, then we switch
     * to "attempting" mode where requests are throttled.
     */
    fun shouldDropRequest(): Boolean {
        return when (shutOffState) {
            State.ATTEMPTING -> randomizer.randomizeSendRequest(config.attemptingThrottle)
            State.SENDING -> false
            State.SHUTOFF -> (shutOffUntil > clock.elapsedRealtime()).also { shutOffStillValid ->
                if (!shutOffStillValid) {
                    shutOffState = State.ATTEMPTING
                    saveState()
                }
            }
        }
    }

}
