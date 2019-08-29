package com.gorilla.attendance.data.model

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.gorilla.attendance.data.db.AttendanceTypeConverters

@Entity
@TypeConverters(AttendanceTypeConverters::class)
class DeviceVideosData {
    @PrimaryKey
    @NonNull
    @SerializedName("deviceToken")
    var deviceToken : String? = null

    @SerializedName("videos")
    var videos : ArrayList<Videos>? = null
}