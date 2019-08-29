package com.gorilla.attendance.data.model

import androidx.room.Entity
import com.google.gson.annotations.SerializedName

@Entity
class AcceptanceIdentifyUser {
    @SerializedName("id")
    var id: String? = null

    @SerializedName("employeeId")
    var employeeId: String? = null

    @SerializedName("mobileNo")
    var mobileNo: String? = null

    @SerializedName("securityCode")
    var securityCode: String? = null

    @SerializedName("rfid")
    var rfid: String? = null

    @SerializedName("firstName")
    var firstName: String? = null

    @SerializedName("lastName")
    var lastName: String? = null

    @SerializedName("photoUrl")
    var photoUrl: String? = null

    @SerializedName("type")
    var type: String? = null
}