package com.gorilla.attendance.data.model

import com.google.gson.annotations.SerializedName

class DeviceMarquees{
    @SerializedName("status")
    var status : String? = null
    @SerializedName("data")
    var data : DeviceMarqueesData? = null
}