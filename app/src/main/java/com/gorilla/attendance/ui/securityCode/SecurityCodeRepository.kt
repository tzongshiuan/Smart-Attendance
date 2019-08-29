package com.gorilla.attendance.ui.securityCode

import com.gorilla.attendance.api.ApiService
import com.gorilla.attendance.data.db.DeviceEmployeeDao
import com.gorilla.attendance.data.db.DeviceVisitorDao
import com.gorilla.attendance.data.model.Acceptances
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class SecurityCodeRepository @Inject constructor(
    private val apiService: ApiService,
    private val deviceEmployeeDao: DeviceEmployeeDao,
    private val deviceVisitorDao: DeviceVisitorDao
) {

    fun verifyEmployee(securityCode : String?) : Single<List<Acceptances>> {
        return deviceEmployeeDao.searchDeviceEmployeeBySecurityCode(securityCode)
    }

    fun verifyVisitor(securityCode : String?) : Single<List<Acceptances>> {
        return deviceVisitorDao.searchDeviceVisitorBySecurityCode(securityCode)
    }

    fun deleteVisitor(acceptances: Acceptances?): Single<Int> {
        return Single.fromCallable { deviceVisitorDao.deleteDeviceVisitor(acceptances) }.subscribeOn(Schedulers.io())
    }
}