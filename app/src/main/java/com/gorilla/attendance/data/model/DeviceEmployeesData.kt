package com.gorilla.attendance.data.model

import com.google.gson.annotations.SerializedName

class DeviceEmployeesData{
    @SerializedName("acceptances")
    var acceptances : ArrayList<Acceptances>? = null
}