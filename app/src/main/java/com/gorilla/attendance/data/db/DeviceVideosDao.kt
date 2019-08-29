package com.gorilla.attendance.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gorilla.attendance.data.model.DeviceVideosData
import io.reactivex.Flowable


@Dao
abstract class DeviceVideosDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertDeviceVideosData(deviceVideosData: DeviceVideosData?)

    @Query("SELECT * FROM DeviceVideosData WHERE deviceToken = :deviceToken")
    abstract fun getDeviceVideosData(deviceToken: String?): Flowable<DeviceVideosData>

    @Query("DELETE FROM DeviceVideosData")
    abstract fun deleteVideosData()
}