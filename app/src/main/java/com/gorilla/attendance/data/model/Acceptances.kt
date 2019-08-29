package com.gorilla.attendance.data.model

import androidx.annotation.NonNull
import androidx.room.Entity
import com.google.gson.annotations.SerializedName

@Entity(primaryKeys = ["id"])
class Acceptances{
    @SerializedName("deviceToken")
    var deviceToken: String? = null

    @NonNull
    @SerializedName("id")
    var id: String? = null

    @SerializedName("employeeId")
    var employeeId: String? = null

    @NonNull
    @SerializedName("securityCode")
    var securityCode: String? = null

    @SerializedName("firstName")
    var firstName: String? = null

    @SerializedName("lastName")
    var lastName: String? = null

    @SerializedName("photoUrl")
    var photoUrl: String? = null

    @SerializedName("intId")
    var intId: Int = 0

    // visitor attributes
    @SerializedName("rfid")
    var rfid: String? = null

    @SerializedName("mobileNo")
    var mobileNo: String? = null

    @SerializedName("startTime")
    var startTime: Long? = null

    @SerializedName("endTime")
    var endTime: Long? = null

    @SerializedName("type")
    var type: String? = null
}