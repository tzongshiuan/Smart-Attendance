package com.gorilla.attendance.data.model

import androidx.room.Entity
import com.google.gson.annotations.SerializedName

@Entity
class FaceImageData {
    @SerializedName("format")
    var format : String? = null

    @SerializedName("dataInBase64")
    var dataInBase64 : String? = null
}