package com.gorilla.attendance.data.model

import com.google.gson.annotations.SerializedName

class DeviceLogin{
    @SerializedName("status")
    var status : String? = null
    @SerializedName("data")
    var data : DeviceLoginData? = null
}