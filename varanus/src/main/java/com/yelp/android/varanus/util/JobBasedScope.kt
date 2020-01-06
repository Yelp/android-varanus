package com.yelp.android.varanus.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Helper class to make classes CoroutineScoped.
 * See here for more of an explanation:
 *
 * https://discuss.kotlinlang.org/t/simpler-coroutine-scope-creation/10833
 */

class JobBasedScope (
    additionalContext: CoroutineContext = EmptyCoroutineContext
) : CoroutineScopeAndJob {
    override val job = Job()
    override val coroutineContext: CoroutineContext = job + additionalContext
}

interface CoroutineScopeAndJob : CoroutineScope {
    val job: Job
}
