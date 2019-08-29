package com.gorilla.attendance.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gorilla.attendance.data.model.DeviceLoginData
import io.reactivex.Flowable


@Dao
abstract class DeviceLoginDao{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertDeviceLoginData(deviceLoginData: DeviceLoginData?)

//    @Query("SELECT * FROM DeviceLoginData WHERE deviceToken = :deviceToken")
//    abstract fun getDeviceLoginData(deviceToken: String): LiveData<DeviceLoginData>

    @Query("SELECT * FROM DeviceLoginData WHERE deviceToken = :deviceToken")
    abstract fun getDeviceLoginData(deviceToken: String?): Flowable<DeviceLoginData>

    @Query("DELETE FROM Acceptances")
    abstract fun deleteAcceptances()
}