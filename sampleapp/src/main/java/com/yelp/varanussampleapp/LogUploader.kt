package com.yelp.varanussampleapp

import com.yelp.android.varanus.LogUploadingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * You would implement a similar class to send logs somewhere of what your network traffic is doing.
 *
 * In this case, we are displaying these summaries in the app, but in reality, you probably want
 * to send them to a server somewhere with some sort of logging or analytics.
 */
class LogUploader(
        private val activity: MonitorLizardActivity
) : LogUploadingManager.LogUploaderBase {

    val foodStats = mapOf(
            "fruit" to Counter(),
            "insect" to Counter(),
            "fish" to Counter(),
            "total" to Counter()
    )

    init {
        activity.updateText(foodStats)
    }

    override suspend fun uploadTrafficLogSummaryForInterval(
            data: Long,
            requests: Int,
            time_interval: Long,
            endpoint: String
    ) {
        foodStats[endpoint]?.apply{ addCount(requests) }?.apply { addSize(data) }
        GlobalScope.launch(Dispatchers.Main) {
            activity.updateText(foodStats)
        }
    }


    data class Counter(var count : Int = 0, var size : Long = 0) {
        fun addCount(newCount: Int) {
            count += newCount
        }
        fun addSize(newSize: Long) {
            size += newSize
        }
    }
}

