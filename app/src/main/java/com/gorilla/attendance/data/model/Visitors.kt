package com.gorilla.attendance.data.model

import com.google.gson.annotations.SerializedName

class Visitors {
    @SerializedName("bapModelId")
    var bapModelId : String? = null
    @SerializedName("id")
    var id : String? = null
    @SerializedName("intId")
    var intId : Int = 0
    @SerializedName("firstName")
    var firstName : String? = null
    @SerializedName("lastName")
    var lastName : String? = null
    @SerializedName("model")
    var model : String? = null
    @SerializedName("createdTime")
    var createdTime : Long = 0
}