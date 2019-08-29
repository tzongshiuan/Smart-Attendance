package com.gorilla.attendance.utils.networkChecker

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import androidx.lifecycle.MutableLiveData

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/2/14
 * Description: record the latest network state
 */
class NetworkChecker(val context: Context) {
    companion object {
        private var isNetworkConnected = MutableLiveData<Boolean>()

        private fun updateNetworkState(context: Context) {
            var isConnected = false

            val localPackageManager = context.packageManager
            if (localPackageManager.checkPermission("android.permission.ACCESS_NETWORK_STATE",
                    context.packageName) != PackageManager.PERMISSION_GRANTED) {
                return
            }

            val localConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifiInfo = localConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            val networks = localConnectivityManager.allNetworks
            for (network in networks) {
                val capabilities = localConnectivityManager.getNetworkCapabilities(network)
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    if (wifiInfo.isConnected) {
                        isConnected = true
                    }
                    break
                }
            }

            isNetworkConnected.value = isConnected
        }

        fun refresh(context: Context) {
            updateNetworkState(context)
        }
    }

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
        return if (connectivityManager is ConnectivityManager) {
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected ?: false
        } else false
    }
}