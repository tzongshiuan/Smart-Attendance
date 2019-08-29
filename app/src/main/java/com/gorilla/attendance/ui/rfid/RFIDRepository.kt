package com.gorilla.attendance.ui.rfid

import com.gorilla.attendance.api.ApiService
import com.gorilla.attendance.data.db.DeviceEmployeeDao
import com.gorilla.attendance.data.db.DeviceVisitorDao
import com.gorilla.attendance.data.model.Acceptances
import io.reactivex.Single
import javax.inject.Inject

class RFIDRepository @Inject constructor(
    private val apiService: ApiService,
    private val deviceEmployeeDao: DeviceEmployeeDao,
    private val deviceVisitorDao: DeviceVisitorDao
) {

    fun verifyEmployee(rfid: String?) : Single<List<Acceptances>> {
        return deviceEmployeeDao.searchDeviceEmployeeByRFID(rfid)
    }

//    fun verifyVisitor(securityCode : String?) : Single<List<Acceptances>> {
//        return deviceVisitorDao.searchDeviceVisitorBySecurityCode(securityCode)
//    }

//    fun deleteVisitor(acceptances: Acceptances?): Single<Int> {
//        return Single.fromCallable { deviceVisitorDao.deleteDeviceVisitor(acceptances) }.subscribeOn(Schedulers.io())
//    }
}