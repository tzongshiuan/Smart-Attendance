package com.gorilla.attendance.data.db

import androidx.room.*
import com.gorilla.attendance.data.model.Acceptances
import io.reactivex.Single


@Dao
abstract class DeviceVisitorDao{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertDeviceVisitorData(deviceLoginData: Acceptances?)


    @Query("SELECT * FROM Acceptances WHERE securityCode = :securityCode")
    abstract fun searchDeviceVisitorBySecurityCode(securityCode: String?): Single<List<Acceptances>>


    @Delete
    abstract fun deleteDeviceVisitor(acceptances: Acceptances?): Int
}