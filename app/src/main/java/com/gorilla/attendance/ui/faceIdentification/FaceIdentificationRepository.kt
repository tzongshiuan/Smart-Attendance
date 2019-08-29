package com.gorilla.attendance.ui.faceIdentification

import com.gorilla.attendance.api.ApiService
import com.gorilla.attendance.data.db.DeviceEmployeeDao
import com.gorilla.attendance.data.db.DeviceVisitorDao
import javax.inject.Inject

class FaceIdentificationRepository @Inject constructor(
    private val apiService: ApiService,
    private val deviceEmployeeDao: DeviceEmployeeDao,
    private val deviceVisitorDao: DeviceVisitorDao
) {
}