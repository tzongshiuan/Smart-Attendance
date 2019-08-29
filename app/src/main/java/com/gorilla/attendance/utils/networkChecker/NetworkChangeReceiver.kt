package com.gorilla.attendance.utils.networkChecker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/2/14
 * Description: to receiver network change event sent from android system
 */
//@Suppress("DEPRECATION")
class NetworkChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            context?.let { NetworkChecker.refresh(it) }
        }
    }
}