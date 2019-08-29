package com.gorilla.attendance.data.model

import com.google.gson.annotations.SerializedName

class LocaleText {
    @SerializedName("zh_TW")
    var zh_TW: String? = null

    @SerializedName("en_US")
    var en_US: String? = null

    @SerializedName("zh_CN")
    var zh_CN: String? = null
}