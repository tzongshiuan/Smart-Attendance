package com.gorilla.attendance.data.model

import com.google.gson.annotations.SerializedName

class BapIdentifyData {
    @SerializedName("type")
    var type : String? = null

    @SerializedName("imageList")
    var imageList : ArrayList<FaceImageData>? = null
}