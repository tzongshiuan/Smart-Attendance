package com.gorilla.attendance.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gorilla.attendance.data.model.DeviceMarqueesData
import io.reactivex.Flowable


@Dao
abstract class DeviceMarqueesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertDeviceMarqueesData(deviceMarqueesData: DeviceMarqueesData?)

    @Query("SELECT * FROM DeviceMarqueesData WHERE deviceToken = :deviceToken")
    abstract fun getDeviceMarqueesData(deviceToken: String?): Flowable<DeviceMarqueesData>

    @Query("DELETE FROM DeviceMarqueesData")
    abstract fun deleteMarqueesData()
}