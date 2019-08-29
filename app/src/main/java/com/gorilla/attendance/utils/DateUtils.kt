package com.gorilla.attendance.utils

import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/7/2
 * Description:
 */
class DateUtils {
    companion object {
        fun nowDateTime2Str(): String {
            val now = Date()
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault())
            return sdf.format(now)
        }

        fun nowDateTimeForIdentities(): String {
            val now = Date()
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("Etc/GMT+0")
            return sdf.format(now)
        }

        fun checkVisitorTime(startTime: Long?, endTime: Long?): Boolean {
            if (startTime == null || endTime == null) {
                Timber.e("Invalid visitor time, startTime or endTime is null")
                return false
            }

            // judge time validity
            val unixTime = System.currentTimeMillis()
            return (unixTime in startTime..endTime)
        }
    }
}