package com.gorilla.attendance.data.model

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.gorilla.attendance.data.db.AttendanceTypeConverters

@Entity
@TypeConverters(AttendanceTypeConverters::class)
class DeviceMarqueesData{
    @PrimaryKey
    @NonNull
    @SerializedName("deviceToken")
    var deviceToken : String? = null

    @SerializedName("marquees")
    var marquees: ArrayList<Marquees>? = null
}