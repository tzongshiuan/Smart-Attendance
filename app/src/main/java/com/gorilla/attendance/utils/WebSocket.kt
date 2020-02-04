package com.gorilla.attendance.utils

import android.content.Context
import io.reactivex.disposables.Disposable
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft
import org.java_websocket.handshake.ServerHandshake
import rx.subjects.PublishSubject
import timber.log.Timber
import java.net.URI

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/7/17
 * Description:
 */
class WebSocket: WebSocketClient {

    companion object {
        const val STATE_WEB_SOCKET_CONNECT = 0
        const val STATE_WEB_SOCKET_DISCONNECT = 1
        const val STATE_CHECK_WEB_SOCKET_ALIVE = 2

        const val PUSH_MESSAGE_SYNC_EMPLOYEE = "SyncEmployee"
        const val PUSH_MESSAGE_SYNC_VISITOR = "SyncVisitor"
        const val PUSH_MESSAGE_SYNC_VIDEO = "SyncVideo"
        const val PUSH_MESSAGE_SYNC_MARQUEE = "SyncMarquee"
        const val PUSH_MESSAGE_SYNC_ALL = "SyncAll"
        const val PUSH_MESSAGE_TEST_CONNECTION = "TestConnection"
        const val PUSH_MESSAGE_RESTART = "Restart"
    }

    private val mContext: Context
    private val mDeviceToken: String
    private val mTimeOut: Long

    private var disconnectSubscriber: Disposable? = null
    private var checkAliveSubscriber: Disposable? = null

    val stateEventSubject = PublishSubject.create<Int>() ?: null
    val syncEventSubject = PublishSubject.create<String>() ?: null

    private var isEnableCounter = false
    private var connectCounter = 0

    constructor(serverUri: URI, draft: Draft, headers: Map<String, String>?, timeOut: Long, context: Context, deviceToken: String)
            : super(serverUri, draft, headers, timeOut.toInt()) {

        Timber.d("serverUri = $serverUri")
        Timber.d("headers = $headers")
        Timber.d("time out = $timeOut")
        Timber.d("context = $context")

        mContext = context
        mDeviceToken = deviceToken
        mTimeOut = timeOut
    }

    private fun sendDisconnectEvent() {
        SimpleRxTask.cancelSubscriber(disconnectSubscriber)
        disconnectSubscriber = SimpleRxTask.createDelaySubscriber(DeviceUtils.CHECK_WEB_SOCKET_CLOSE_ERROR_TIME) {
            stateEventSubject?.onNext(STATE_WEB_SOCKET_DISCONNECT)
        }
    }

    fun sendCheckAliveEvent() {
        SimpleRxTask.cancelSubscriber(checkAliveSubscriber)
        checkAliveSubscriber = SimpleRxTask.createDelaySubscriber(DeviceUtils.CHECK_WEB_SOCKET_TIME) {
            stateEventSubject?.onNext(STATE_CHECK_WEB_SOCKET_ALIVE)
        }
    }

    private fun sendMessage(message: String) {
        //Timber.d("sendMessage(), message: $message")
        send(message)
    }

    override fun onOpen(handshakeData: ServerHandshake?) {
        Timber.d("onOpen(), handshakeData: $handshakeData, deviceToken: $mDeviceToken")

        sendMessage("deviceToken $mDeviceToken")

        SimpleRxTask.cancelSubscriber(disconnectSubscriber)

        // must send CONNECT event after receive valid socket message from server
        startConnectCounter()
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Timber.d("onClose(), code: $code, reason: $reason, remote: $remote")
        sendDisconnectEvent()
    }

    override fun onError(ex: Exception?) {
        sendDisconnectEvent()
    }

    override fun onMessage(message: String?) {
        if (message == null) {
            Timber.d("message == null")
            return
        }

        if (!message.contains("map_session_id")) {
            Timber.i("onMessage(), web socket message: $message")
        }

        if (message.contains("map_session_id")) {
            if (message.contains("0")) {
                sendMessage("map_session_id ok")
            } else {
                // Disconnect
                sendDisconnectEvent()
            }

            if (isEnableCounter) {
//                connectCounter--
//                if (connectCounter <= 0) {
                    isEnableCounter = false
                    stateEventSubject?.onNext(STATE_WEB_SOCKET_CONNECT)
//                }
            }
        } else {
            sendMessage("$message ok")
            syncEventSubject?.onNext(message)
        }
    }

    private fun startConnectCounter() {
        isEnableCounter = true
        connectCounter = 5
    }
}