package com.gorilla.attendance.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gorilla.attendance.data.model.Acceptances
import io.reactivex.Single


@Dao
abstract class DeviceEmployeeDao{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertDeviceEmployeeData(deviceLoginData: Acceptances?)


    @Query("SELECT * FROM Acceptances WHERE securityCode = :securityCode")
    abstract fun searchDeviceEmployeeBySecurityCode(securityCode: String?): Single<List<Acceptances>>

    @Query("SELECT * FROM Acceptances WHERE rfid = :rfid")
    abstract fun searchDeviceEmployeeByRFID(rfid: String?): Single<List<Acceptances>>

    @Query("SELECT * FROM Acceptances WHERE intId = :intId")
    abstract fun searchDeviceEmployeeByIntId(intId: Int?): Single<List<Acceptances>>

//    @Query("SELECT CASE WHEN EXISTS(SELECT * FROM Acceptances WHERE securityCode = :securityCode) THEN CAST(1 AS BIT) ELSE CAST(0 AS BIT) END")
//    abstract fun searchDeviceEmployeeBySecurityCode(securityCode: String?): Flowable<Boolean>
}