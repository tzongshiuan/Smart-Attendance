package com.gorilla.attendance.ui.setting

import com.gorilla.attendance.api.ApiService
import com.gorilla.attendance.data.db.DeviceLoginDao
import javax.inject.Inject

class SettingRepository{
    private val apiService: ApiService
    private val deviceLoginDao: DeviceLoginDao

    @Inject
    constructor(apiService: ApiService, deviceLoginDao: DeviceLoginDao) {
        this.apiService = apiService
        this.deviceLoginDao = deviceLoginDao
    }
}