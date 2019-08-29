package com.gorilla.attendance.data.model

import com.google.gson.annotations.SerializedName

class DeviceUnrecognizedData{
    @SerializedName("deviceToken")
    var deviceToken : String? = null

    @SerializedName("errorLogs")
    val errorLogs : ArrayList<ClockData> = ArrayList()
}