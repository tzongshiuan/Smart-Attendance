package com.gorilla.attendance.data.model

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.gorilla.attendance.data.db.AttendanceTypeConverters

@Entity
@TypeConverters(AttendanceTypeConverters::class)
class DeviceLoginData{
    @PrimaryKey
    @NonNull
    @SerializedName("deviceToken")
    var deviceToken : String? = null
    @SerializedName("locale")
    var locale : String? = null
    @SerializedName("deviceName")
    var deviceName : String? = null
    @SerializedName("modulesModes")
    var modulesModes : ArrayList<ModulesModes>? = null
}