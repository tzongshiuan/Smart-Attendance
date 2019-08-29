package com.gorilla.attendance.ui.common

import androidx.test.espresso.IdlingResource
import timber.log.Timber

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/8/6
 * Description:
 */
class FetchingIdlingResource: IdlingResource, FetcherListener {

    private var idle = true
    private var resourceCallback: IdlingResource.ResourceCallback? = null

    override fun getName(): String {
        return FetchingIdlingResource::class.java.simpleName
    }

    override fun isIdleNow() = idle

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        resourceCallback = callback
    }

    override fun beginFetching() {
        Timber.d("beginFetching()")
        idle = false
    }

    override fun doneFetching() {
        Timber.d("doneFetching()")
        idle = true
        resourceCallback?.onTransitionToIdle()
    }
}