package com.gorilla.attendance.data.model

import com.google.gson.annotations.SerializedName

class DeviceVideos{
    @SerializedName("status")
    var status : String? = null
    @SerializedName("data")
    var data : DeviceVideosData? = null
}