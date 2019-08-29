package com.gorilla.attendance.utils

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.os.Parcelable
import timber.log.Timber
import javax.inject.Inject

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/7/31
 * Description:
 */
class UsbRelayManager @Inject constructor(private val mPreferences: AppPreferences) {

    companion object {
        //private const val USB_BAUD_RATE = 9600

        private const val VENDOR_ID = 0x1A86
        private const val PRODUCT_ID = 0x7523

        private const val ACTION_USB_PERMISSION = "com.gorilla.attendance.USB_RELAY_PERMISSION"
    }

    private val mLOCK = "UsbRelayManager - lock"

    private var mUSBManager: UsbManager? = null
    private var mUsbConnection: UsbDeviceConnection? = null
    private var mContext: Context? = null

    private var mOutEndpoint: UsbEndpoint? = null

    private var mPermissionIntent: PendingIntent? = null

    private var isStarting = false

    fun initUsbRelayManager(context: Context) {
        Timber.d("initNfcManager()")

        mContext = context

        mUSBManager = mContext?.getSystemService(Context.USB_SERVICE) as UsbManager
        mPermissionIntent = PendingIntent.getBroadcast(
            mContext, 0, Intent(
                ACTION_USB_PERMISSION
            ), 0
        )
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
            Timber.d("No connected usb relay device")
            return
        }

        if (!isDeviceSupported(device)) {
            return
        }

        if (mUSBManager?.hasPermission(device) != true) {
            Timber.d("USB requestPermission")
            mUSBManager?.requestPermission(device, mPermissionIntent)
        } else {
            openUsbDevice(device)
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

        try {
            mContext?.unregisterReceiver(mReceiver)
        } catch (e: Exception) {
            Timber.e("stop failed, message: ${e.message}")
        }
    }

    private val mReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("onReceive(), action: ${intent.action}")

            val action = intent.action

            when {
                ACTION_USB_PERMISSION == action -> synchronized(mLOCK) {
                    val device = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice
                    if (!isDeviceSupported(device)) {
                        return
                    }

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Timber.d("Request permission success")
                        openUsbDevice(device)
                    } else {
                        Timber.d("Request Manifest.permission failed, do nothing")
                    }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED == action -> synchronized(mLOCK) {
                    //nfcReadStateEvent.postValue(NFC_END_INIT_READER)
                    Timber.d("Usb device detached")
                }

                UsbManager.ACTION_USB_DEVICE_ATTACHED == action -> {
                    val device = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice
                    Timber.d("Usb device attached")
                    Timber.d("device name = ${device.deviceName}")
                    Timber.d("device vendorId = ${device.vendorId.toString(16)}")
                    Timber.d("device productId = ${device.productId.toString(16)}")
                    Timber.d("device productName = ${device.productName}")

                    if (!isDeviceSupported(device)) {
                        return
                    }

                    if (mUSBManager?.hasPermission(device) != true) {
                        mUSBManager?.requestPermission(device, mPermissionIntent)
                    } else {
                        openUsbDevice(device)
                    }
                }
            }
        }
    }

    private fun isDeviceSupported(device: UsbDevice): Boolean {
        if (device.vendorId != VENDOR_ID || device.productId != PRODUCT_ID) {
            Timber.d("This device is not supported !!")
            return false
        }

        return true
    }

    private fun openUsbDevice(device: UsbDevice) {
        Timber.d("openUsbDevice(), device product name: ${device.productName}")

        mUsbConnection = mUSBManager?.openDevice(device)
        mOutEndpoint = device.getInterface(0).getEndpoint(1)

        /**
         * Reference: https://github.com/felHR85/SerialPortExample
         */
        // set baud rate 9600
        mUsbConnection?.controlTransfer(0x40, 0x9A, 0x1312, 0xb282, null, 0, 0)
        // set parity none
        mUsbConnection?.controlTransfer(0x40, 0x9A, 0x2518, 0xc3, null, 0, 0)
    }

    fun openDoor() {
        Timber.d("[USB Relay] openDoor()")

        val openData = byteArrayOf(0xA0.toByte(), 0x01.toByte(), 0x01.toByte(), 0xA2.toByte())
        mUsbConnection?.bulkTransfer(mOutEndpoint!!, openData, 4, 0)

        SimpleRxTask.after(mPreferences.closeDoorTimeout * 1000) {
            closeDoor()
        }
    }

    fun closeDoor() {
        Timber.d("[USB Relay] closeDoor()")

        val closeData = byteArrayOf(0xA0.toByte(), 0x01.toByte(), 0x00.toByte(), 0xA1.toByte())
        mUsbConnection?.bulkTransfer(mOutEndpoint!!, closeData, 4, 0)
    }
}