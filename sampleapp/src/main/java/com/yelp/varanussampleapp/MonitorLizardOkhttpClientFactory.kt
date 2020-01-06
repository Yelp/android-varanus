package com.yelp.varanussampleapp

import com.google.android.gms.common.util.DefaultClock
import com.yelp.android.varanus.NetworkMonitor
import com.yelp.android.varanus.okhttp.TrafficMonitorInterceptor
import com.yelp.android.varanus.shutoff.NetworkShutoffManager
import com.yelp.varanussampleapp.persistence.PersistentDataRepo
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit


private const val SPECIFIC_ENDPOINT_ERROR_CODE = 555
private const val ALL_ENDPOINT_ERROR_CODE = 556
private const val MAX_BACKOFF_MULTIPLYER = 12 // 12 * 5 = 1 minute
private const val THROTTLE_AMOUNT = 10 // drop 9 in 10 requests when trying again

/**
 * In order to support any network library you might use, we have pulled out the OkHttp related
 * content.
 *
 * We set up the OkHttp client, instantiate the network monitoring classes, then, using an
 * interceptor, set all traffic to go through Varanus.
 *
 * Varanus works particularly well with OkHttp because of OkHttp's interceptor support, but in
 * principle you could implement something similar with any library.
 */
object MonitorLizardOkhttpClientFactory {

    fun configureOkhttpClient(activity: MonitorLizardActivity, alertIssuer: LogUploader): OkHttpClient {

        val persistentDataRepo = PersistentDataRepo.getPersistentRepo(activity)

        val shutoffConfig = NetworkShutoffManager.Config(
                TimeUnit.SECONDS.toMillis(5), // Probably should be minutes in real life
                MAX_BACKOFF_MULTIPLYER,
                THROTTLE_AMOUNT,
                SPECIFIC_ENDPOINT_ERROR_CODE,
                ALL_ENDPOINT_ERROR_CODE,
                TimeUnit.SECONDS.toMillis(2))

        val networkMonitor = NetworkMonitor(
                TimeUnit.SECONDS.toMillis(5),
                TimeUnit.SECONDS.toMillis(5),
                persistentDataRepo.networkTrafficLogPersister,
                alertIssuer
        )

        val networkShutoffManager = NetworkShutoffManager(
                DefaultClock.getInstance(), // This exists for testing reasons
                NetworkShutoffManager.Randomizer(shutoffConfig), // This exists for testing reasons
                persistentDataRepo.networkShutoffLogPersister,
                shutoffConfig
        )
        activity.shutoffManager = networkShutoffManager // So that we can print the status

        val endpointKeyExtractor = AppEndpointKeyExtractor()

        return OkHttpClient.Builder().addInterceptor(
                TrafficMonitorInterceptor(
                        networkMonitor,
                        endpointKeyExtractor,
                        networkShutoffManager
                )).addInterceptor(FakeCdnInterceptor(endpointKeyExtractor))
                .build()
    }
}