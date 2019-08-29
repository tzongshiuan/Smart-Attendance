package com.gorilla.attendance.utils

import android.content.Context
import org.java_websocket.drafts.Draft_17
import rx.Observer
import rx.Subscription
import timber.log.Timber
import java.net.URI
import javax.inject.Inject

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/7/17
 * Description:
 */
class WebSocketManager @Inject constructor(private val mPreferences: AppPreferences) {

    var mWebSocket: WebSocket? = null

    val webSocketStateEvent = SingleLiveEvent<Int>()
    val webSocketSyncEvent = SingleLiveEvent<String>()

    private var stateEventSubscriber: Subscription? = null
    private var syncEventSubscriber: Subscription? = null

    private val stateEventObserver = object: Observer<Int> {
        override fun onError(e: Throwable?) {
        }

        override fun onNext(state: Int?) {
            webSocketStateEvent.postValue(state)
        }

        override fun onCompleted() {
        }
    }

    private val syncEventObserver = object: Observer<String> {
        override fun onError(e: Throwable?) {
        }

        override fun onNext(syncData: String?) {
            //Timber.d("onNext: $syncData")
            webSocketSyncEvent.postValue(syncData)
        }

        override fun onCompleted() {
        }
    }

    fun connect(serverUri: URI, timeOut: Long, context: Context) {
        Timber.d("connect webSocket start!!")
        Timber.d("Connect to serverUri = $serverUri")
        Timber.d("Connect time out = $timeOut")

        if (mWebSocket == null) {
            mWebSocket = WebSocket(serverUri, Draft_17(), null, timeOut, context, mPreferences.tabletToken)

            stateEventSubscriber = mWebSocket?.stateEventSubject?.subscribe(stateEventObserver)
            syncEventSubscriber = mWebSocket?.syncEventSubject?.subscribe(syncEventObserver)
        }

        try {
            mWebSocket?.connect()
        } catch (e: Exception) {
            Timber.e("WebSocket connect exception, message: ${e.message}")
        }
    }

    @Synchronized
    fun reconnect(serverUri: URI, timeOut: Long, context: Context) {
        Timber.d("reconnect webSocket start!!")
        disconnect()
        connect(serverUri, timeOut, context)
    }

    @Synchronized
    fun disconnect() {
        Timber.d("disconnect webSocket start!!")

        if (mWebSocket != null) {
            stateEventSubscriber?.unsubscribe()
            syncEventSubscriber?.unsubscribe()
            mWebSocket?.close()
            mWebSocket = null
        }
    }
}