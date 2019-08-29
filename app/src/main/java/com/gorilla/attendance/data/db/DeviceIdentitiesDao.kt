package com.gorilla.attendance.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gorilla.attendance.data.model.AttendanceDeviceInfoData
import com.gorilla.attendance.data.model.DeviceIdentitiesData
import io.reactivex.Flowable
import io.reactivex.Single


@Dao
abstract class DeviceIdentitiesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertDeviceIdentitiesData(deviceIdentitiesData: DeviceIdentitiesData?)

    @Query("SELECT * FROM DeviceIdentitiesData WHERE deviceToken = :deviceToken")
    abstract fun getDeviceIdentitiesData(deviceToken: String?): Flowable<DeviceIdentitiesData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertDeviceInfoData(deviceInfoData: AttendanceDeviceInfoData?)

    @Query("SELECT * FROM AttendanceDeviceInfoData WHERE deviceToken = :deviceToken")
    abstract fun searchDeviceByDeviceToken(deviceToken: String?): Single<List<AttendanceDeviceInfoData>>
}