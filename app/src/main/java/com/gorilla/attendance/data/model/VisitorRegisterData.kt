package com.gorilla.attendance.data.model

import com.google.gson.annotations.SerializedName

class VisitorRegisterData {
    @SerializedName("deviceToken")
    var deviceToken: String? = null

    @SerializedName("mobileNo")
    var mobileNo: String? = null

    @SerializedName("name")
    var name: String? = null

    @SerializedName("company")
    var company: String? = null

    @SerializedName("title")
    var title: String? = null

    @SerializedName("email")
    var email: String? = null

    @SerializedName("rfid")
    var rfid: String? = null

    @SerializedName("securityCode")
    var securityCode: String? = null

    @SerializedName("createTime")
    var createTime: String? = null

    @SerializedName("imageList")
    var imageList: ArrayList<FaceImageData>? = null
}