package com.gorilla.attendance.service

import com.google.android.vending.expansion.downloader.impl.DownloaderService
import com.gorilla.attendance.utils.DeviceUtils
import timber.log.Timber

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/6/26
 * Description:
 */
class ObbDownloaderService: DownloaderService() {
    private val salt = byteArrayOf(1, 43, -12, -1, 54, 98, -100, -12, 43, 2, -8, -4, 9, 5, -106, -108, -33, 45, -1, 84)

    override fun getPublicKey(): String {
        return DeviceUtils.PUBLIC_KEY
    }

    override fun getSALT(): ByteArray {
        return salt
    }

    override fun getAlarmReceiverClassName(): String {
        return ObbDownloadAlarmReceiver::class.java.name
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy()")
        cancelNotification()
    }
}