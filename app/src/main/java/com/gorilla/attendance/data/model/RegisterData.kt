package com.gorilla.attendance.data.model

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.gorilla.attendance.data.db.AttendanceTypeConverters

@Entity
@TypeConverters(AttendanceTypeConverters::class)
class RegisterData{
    @PrimaryKey
    @NonNull
    @SerializedName("deviceToken")
    var deviceToken : String? = null
    @SerializedName("employeeId")
    var employeeId : String? = null
    @SerializedName("name")
    var name : String? = null
    @SerializedName("email")
    var email : String? = null
    @SerializedName("password")
    var password : String? = null
    @SerializedName("createTime")
    var createTime : String? = null
    @SerializedName("format")
    var format : String? = null
    @SerializedName("dataInBase64")
    var dataInBase64 : ByteArray? = null
    @SerializedName("mobileNo")
    var mobileNo : String? = null
    @SerializedName("department")
    var department : String? = null
    @SerializedName("title")
    var title : String? = null
    @SerializedName("modelId")
    var modelId : Int = -1
    @SerializedName("model")
    var model : ByteArray? = null
    @SerializedName("rfid")
    var rfid : String? = null

    @SerializedName("registerType")
    var registerType : Int = 0
    @SerializedName("isSearchUserSuccess")
    var isSearchUserSuccess : Boolean = false
    @SerializedName("securityCode")
    var securityCode : String? = null
}