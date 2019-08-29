package com.gorilla.attendance.data.model

import com.google.gson.annotations.SerializedName

class DeviceRecordsData{
    @SerializedName("deviceToken")
    var deviceToken : String? = null

    @SerializedName("records")
    val records : ArrayList<ClockData> = ArrayList()
}