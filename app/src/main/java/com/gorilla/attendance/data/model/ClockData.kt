package com.gorilla.attendance.data.model

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.gorilla.attendance.ui.main.FaceVerifyResultView

@Entity
class ClockData {
    @SerializedName("deviceToken")
    var deviceToken : String = ""

    @SerializedName("serial")
    var serial : Int = 0

    @SerializedName("id")
    var id : String? = null

    @SerializedName("securityCode")
    var securityCode : String? = null

    @SerializedName("type")
    var type : String? = null

    @SerializedName("faceImg")
    var faceImg : String? = null

    @SerializedName("faceVerify")
    var faceVerify : String? = null

    @PrimaryKey
    @NonNull
    @SerializedName("deviceTime")
    var deviceTime : String? = null

    @SerializedName("clockType")
    var clockType : Int? = null

    @SerializedName("liveness")
    var liveness : String? = null

    @SerializedName("mode")
    var mode : Int = 0

    @SerializedName("module")
    var module : Int = 0

    @SerializedName("rfid")
    var rfid : String? = null

    @SerializedName("recordMode")
    var recordMode : String? = null

//    @SerializedName("isVisitorOpenDoor")
//    var isVisitorOpenDoor : Boolean = false
//    @SerializedName("isEmployeeOpenDoor")
//    var isEmployeeOpenDoor : Boolean = false

    @SerializedName("firstName")
    var firstName : String? = null

    @SerializedName("lastName")
    var lastName : String? = null

    @SerializedName("intId")
    var intId : Int = 0

    constructor()

    constructor(clockData: ClockData?) {
        deviceToken = clockData?.deviceToken ?: ""
        serial = clockData?.serial ?: 0
        id = clockData?.id
        securityCode = clockData?.securityCode
        type = clockData?.type
        faceImg = clockData?.faceImg
        faceVerify = clockData?.faceVerify
        deviceTime = clockData?.deviceTime
        clockType = clockData?.clockType
        liveness = clockData?.liveness
        mode = clockData?.mode ?: 0
        module = clockData?.module ?: 0
        rfid = clockData?.rfid
        recordMode = clockData?.recordMode
        firstName = clockData?.firstName
        lastName = clockData?.lastName
        intId = clockData?.intId ?: 0
    }
}