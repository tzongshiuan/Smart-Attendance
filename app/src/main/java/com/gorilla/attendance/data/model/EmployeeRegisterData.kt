package com.gorilla.attendance.data.model

import com.google.gson.annotations.SerializedName

class EmployeeRegisterData {
    @SerializedName("deviceToken")
    var deviceToken: String? = null

    @SerializedName("employeeId")
    var employeeId: String? = null

    @SerializedName("name")
    var name: String? = null

    @SerializedName("email")
    var email: String? = null

    @SerializedName("password")
    var password: String? = null

    @SerializedName("rfid")
    var rfid: String? = null

    @SerializedName("securityCode")
    var securityCode: String? = null

    @SerializedName("createTime")
    var createTime: String? = null

    @SerializedName("imageList")
    var imageList: ArrayList<FaceImageData>? = null
}