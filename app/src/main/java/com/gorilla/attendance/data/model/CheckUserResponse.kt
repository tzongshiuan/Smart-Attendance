package com.gorilla.attendance.data.model

import com.google.gson.annotations.SerializedName

class CheckUserResponse{
    @SerializedName("status")
    var status: String? = null

    @SerializedName("error")
    var error: ErrorData? = null

    @SerializedName("data")
    var data: Acceptances? = null
}