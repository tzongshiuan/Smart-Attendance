package com.gorilla.attendance.data.model

import com.google.gson.annotations.SerializedName

class ModulesModes{
    @SerializedName("module")
    var module : Int = 0
    @SerializedName("modes")
    var modes : Array<Int>? = null
}