package com.gorilla.attendance.data.model

import com.google.gson.annotations.SerializedName

class Employees {
    @SerializedName("bapModelId")
    var bapModelId : String? = null
    @SerializedName("id")
    var id : String? = null
    @SerializedName("intId")
    var intId : Int = 0
    @SerializedName("employeeId")
    var employeeId : String? = null
    @SerializedName("firstName")
    var firstName : String? = null
    @SerializedName("lastName")
    var lastName : String? = null
    @SerializedName("model")
    var model : String? = null
    @SerializedName("createdTime")
    var createdTime : Long = 0

    override fun equals(other: Any?): Boolean {
        return (other as Employees).intId == this.intId
    }

    override fun hashCode(): Int {
        return intId
    }
}