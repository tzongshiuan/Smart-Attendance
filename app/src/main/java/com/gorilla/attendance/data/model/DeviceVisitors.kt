package com.gorilla.attendance.data.model

import com.google.gson.annotations.SerializedName

class DeviceVisitors {
    @SerializedName("status")
    var status : String? = null
    @SerializedName("data")
    var data : DeviceVisitorsData? = null
}