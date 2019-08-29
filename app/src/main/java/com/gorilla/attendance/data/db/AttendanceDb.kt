package com.gorilla.attendance.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gorilla.attendance.data.model.*

@Database(
    entities = [
        DeviceLoginData::class,
        Acceptances::class,
        ClockData::class,
        DeviceIdentitiesData::class,
        DeviceMarqueesData::class,
        DeviceVideosData::class,
        AttendanceDeviceInfoData::class],
    version = 9,
    exportSchema = false
)

abstract class AttendanceDb: RoomDatabase() {
    abstract fun deviceLoginDao(): DeviceLoginDao
    abstract fun deviceEmployeeDao(): DeviceEmployeeDao
    abstract fun deviceVisitorDao(): DeviceVisitorDao
    abstract fun clockDao(): ClockDao
    abstract fun deviceIdentitiesDao(): DeviceIdentitiesDao
    abstract fun deviceMarqueesDao(): DeviceMarqueesDao
    abstract fun deviceVideosDao(): DeviceVideosDao
}