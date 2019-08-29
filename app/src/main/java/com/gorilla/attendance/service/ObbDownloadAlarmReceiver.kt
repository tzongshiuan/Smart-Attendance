package com.gorilla.attendance.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.google.android.vending.expansion.downloader.DownloaderClientMarshaller

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/6/26
 * Description:
 */
class ObbDownloadAlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            DownloaderClientMarshaller.startDownloadServiceIfRequired(context, intent, ObbDownloaderService::class.java)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }
}