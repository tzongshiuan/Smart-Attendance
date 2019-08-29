package com.gorilla.attendance.data.model

import com.google.gson.annotations.SerializedName

class Marquees {
    @SerializedName("name")
    var name: String? = null

    @SerializedName("text")
    var text: LocaleText? = null

    @SerializedName("speed")
    var speed: Int = 1

    @SerializedName("direction")
    var direction: Int = 1
}