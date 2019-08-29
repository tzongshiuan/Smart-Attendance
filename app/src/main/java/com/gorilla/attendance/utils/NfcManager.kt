package com.gorilla.attendance.utils

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Parcelable
import android.text.TextUtils
import android.widget.Toast
import com.acs.smartcard.Reader
import timber.log.Timber

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/7/16
 * Description:
 */
class NfcManager {

    companion object {
        const val NFC_START_INIT_READER = 0
        const val NFC_START_CARD_READ = 1
        const val NFC_END_INIT_READER = 2
        const val NFC_END_CARD_READ = 3
        const val NFC_PERMISSION_DENIED = 4
        const val NFC_DEVICE_NOT_FOUND = 5
        const val NFC_READER_NOT_SUPPORT = 6

        private const val STATUS_OPEN = 50001
        private const val STATUS_CLOSE = 50002
        private const val STATUS_READING_CARD = 50003

        private const val READER_CONTROL_CODE = 3500

        private const val ACTION_USB_PERMISSION = "com.gorilla.attendance.USB_PERMISSION"
    }

    private val mLOCK = "NFCManager - lock"

    private var mUSBManager: UsbManager? = null
    private var mContext: Context? = null
    private var mReader: Reader? = null
    private var mPermissionIntent: PendingIntent? = null
    private var mCurrentStatus: Int = STATUS_CLOSE

    private val readCommand = byteArrayOf(0xFF.toByte(), 0xCA.toByte(), 0x00, 0x00, 0x00)

    private var mEnableReadCard = true

    val nfcReadStateEvent = SingleLiveEvent<Int>()
    val nfcReadData = SingleLiveEvent<String?>()

    private var isInitCardStateChange = true

    private var isStarting = false

    private var isUserDenyPermission = false

    fun initNfcManager(context: Context) {
        Timber.d("initNfcManager()")

        mContext = context

        mUSBManager = mContext?.getSystemService(Context.USB_SERVICE) as UsbManager
        mReader = Reader(mUSBManager)
        mReader?.setOnStateChangeListener(mOnStateChangeListener)
        mPermissionIntent = PendingIntent.getBroadcast(
            mContext, 0, Intent(
                ACTION_USB_PERMISSION
            ), 0
        )

        isUserDenyPermission = false
    }

    fun setEnableReadCard(enableReadCard: Boolean) {
        mEnableReadCard = enableReadCard
    }

    fun start() {
        Timber.d("start()")

        if (isStarting) {
            Timber.d("Already start, do nothing")
            return
        }
        isStarting = true

        //stop()
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        mContext?.registerReceiver(mReceiver, filter)
        val device = getUsbDevice()

        if (device == null) {
            Timber.d("No connected NFC reader")
            Toast.makeText(mContext, "No connected NFC reader", Toast.LENGTH_SHORT).show()
            nfcReadStateEvent.postValue(NFC_DEVICE_NOT_FOUND)
        }

//        if (mReader?.isSupported(device)!!) {
//            Timber.d("NFC reader is not supported")
//            Toast.makeText(mContext, "NFC reader is not supported", Toast.LENGTH_SHORT).show()
//            nfcReadStateEvent.postValue(NFC_READER_NOT_SUPPORT)
//        }

        if (device != null && mReader?.isSupported(device)!!) {
            Timber.d("USB requestPermission")
            //Toast.makeText(mContext, "usb requestPermission", Toast.LENGTH_SHORT).show()
            mUSBManager?.requestPermission(device, mPermissionIntent)
        }
    }

    private fun getUsbDevice(): UsbDevice? {
        for (device in mUSBManager?.deviceList?.values!!) {
            return device
        }
        return null
    }

    fun stop() {
        Timber.d("stop()")

        if (!isStarting) {
            Timber.d("Never start, return and do nothing")
            return
        }
        isStarting = false

        isInitCardStateChange = true

        try {
            mContext?.unregisterReceiver(mReceiver)
        } catch (e: Exception) {
            Timber.e("stop failed, message: ${e.message}")
        }

        mReader?.close()
        mCurrentStatus = STATUS_CLOSE
    }

    private val mOnStateChangeListener = object: Reader.OnStateChangeListener {
        override fun onStateChange(slotNum: Int, prevState: Int, currState: Int) {
            var preState = prevState
            var curState = currState

//            Timber.d(
//                "mOnStateChangeListener, currState: $curState, preState: $preState, mCurrentStatus: $mCurrentStatus, mEnableReadCard: $mEnableReadCard"
//            )

            if (isInitCardStateChange) {
                isInitCardStateChange = false
                return
            }

            if (preState < Reader.CARD_UNKNOWN || preState > Reader.CARD_SPECIFIC) {
                preState = Reader.CARD_UNKNOWN
            }

            if (curState < Reader.CARD_UNKNOWN || curState > Reader.CARD_SPECIFIC) {
                curState = Reader.CARD_UNKNOWN
            }

            Timber.d(
                "mOnStateChangeListener, currState: $curState, preState: $preState, mCurrentStatus: $mCurrentStatus, mEnableReadCard: $mEnableReadCard"
            )

            if (curState == Reader.CARD_PRESENT && preState != curState && mCurrentStatus == STATUS_OPEN && mEnableReadCard) {
                mCurrentStatus = STATUS_READING_CARD
                nfcReadStateEvent.postValue(NFC_START_CARD_READ)
                readCard()
            }
        }
    }

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("onReceive(), action: ${intent.action}")

            val action = intent.action

            when {
                ACTION_USB_PERMISSION == action -> synchronized(mLOCK) {
                    val device = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice
                    if (!mReader?.isSupported(device)!!) {
                        return
                    }

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Timber.d("Request permission success")
                        nfcReadStateEvent.postValue(NFC_START_INIT_READER)
                        isInitCardStateChange = true
                        deviceOpen(device)
                    } else {
                        Timber.d("Request Manifest.permission failed, do nothing")
                        Toast.makeText(mContext, "Request permission failed, do nothing", Toast.LENGTH_SHORT).show()
                        isUserDenyPermission = true
                        nfcReadStateEvent.postValue(NFC_PERMISSION_DENIED)
                    }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED == action -> synchronized(mLOCK) {
//                    val device = intent
//                        .getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice
//
//                    if (device == mReader?.device) {

                        nfcReadStateEvent.postValue(NFC_END_INIT_READER)
                        Timber.d("Usb device detached")

                        //mHandler.sendEmptyMessage(MSG_DISCONNECT)

                        deviceClose()
//                    }
                }

                UsbManager.ACTION_USB_DEVICE_ATTACHED == action -> {
                    val device = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice

                    Timber.d("Usb device attached")

                    if (mCurrentStatus == STATUS_CLOSE && mReader?.isSupported(device)!!) {
                        mUSBManager?.requestPermission(device, mPermissionIntent)
                    }
                }
            }
        }
    }

    private fun deviceOpen(device: UsbDevice) {
        Timber.d("deviceOpen()")

        var openResult = false
        try {
            mReader?.open(device)
            openResult = true
        } catch (e: Exception) {
            Timber.e("deviceOpen failed, message: ${e.message}")
        }

        Timber.d("openResult: $openResult, mCurrentStatus: $mCurrentStatus, mReader?.isOpened: ${mReader?.isOpened!!}")
        if (openResult && mCurrentStatus == STATUS_CLOSE && mReader?.isOpened!!) {
            mCurrentStatus = STATUS_OPEN
        }
    }

    private fun deviceClose() {
        Timber.d("deviceClose()")

        var closeResult = false

        try {
            mReader?.close()
            closeResult = true
        } catch (e: Exception) {
            Timber.e("deviceClose failed, message: ${e.message}")
        }

        if (closeResult && !mReader?.isOpened!!) {
            mCurrentStatus = STATUS_CLOSE
        }
    }

    private fun readCard() {
        Timber.d("readCard()")

        val response = ByteArray(500)
        var responseLength: Int

        try {
            responseLength = mReader?.control(
                0, READER_CONTROL_CODE, readCommand, readCommand.size, response, response.size) ?: 0

            Timber.d("Before cut: ${byteArrayToString(response, responseLength)}")
            // skip tail response
            if (responseLength >= 2) {
                responseLength -= 2
            }
            Timber.d("After cut: ${byteArrayToString(response, responseLength)}")

            mCurrentStatus = if (mReader?.isOpened!!) {
                STATUS_OPEN
            } else {
                STATUS_CLOSE
            }

            nfcReadData.postValue(byteArrayToString(response, responseLength))
            return
        } catch (e: Exception) {
            Timber.e("readCard failed, message: ${e.message}")
        }

        nfcReadData.postValue(null)
    }

    private fun byteArrayToString(buffer: ByteArray, bufferLength: Int): String? {
        var bufferString = ""

        for (i in 0 until bufferLength) {
            var hexChar = Integer.toHexString((buffer[i].toInt()) and 0xFF)
            if (hexChar.length == 1) {
                hexChar = "0$hexChar"
            }
            bufferString += hexChar.toUpperCase()
        }

        return if (!TextUtils.isEmpty(bufferString)) {
            bufferString
        } else {
            null
        }
    }
}