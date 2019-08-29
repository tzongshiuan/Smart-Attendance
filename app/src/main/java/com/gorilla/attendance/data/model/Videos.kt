package com.gorilla.attendance.data.model

import com.google.gson.annotations.SerializedName

class Videos {
    @SerializedName("name")
    var name: String? = null

    @SerializedName("url")
    var url: String? = null

    @SerializedName("thumbUrl")
    var thumbUrl: String? = null

    @SerializedName("priority")
    var priority: Int = 1

    @SerializedName("length")
    var length: Int? = null

    @SerializedName("fileSize")
    var fileSize: Long? = null
}