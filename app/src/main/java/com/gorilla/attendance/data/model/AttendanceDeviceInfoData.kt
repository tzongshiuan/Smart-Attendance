package com.gorilla.attendance.data.model

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.gorilla.attendance.data.db.AttendanceTypeConverters

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/8/20
 * Description:
 */
@Entity
@TypeConverters(AttendanceTypeConverters::class)
class AttendanceDeviceInfoData {
    @NonNull
    @SerializedName("serverIp")
    var serverIp: String? = null

    @PrimaryKey
    @NonNull
    @SerializedName("deviceToken")
    var deviceToken: String? = null

    @NonNull
    @SerializedName("updateTime")
    var updateTime: String? = null
}