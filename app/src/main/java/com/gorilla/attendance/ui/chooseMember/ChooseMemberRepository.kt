package com.gorilla.attendance.ui.chooseMember

import com.google.gson.Gson
import com.gorilla.attendance.api.ApiService
import com.gorilla.attendance.data.db.DeviceLoginDao
import com.gorilla.attendance.data.model.DeviceEmployees
import com.gorilla.attendance.data.model.DeviceIdentities
import com.gorilla.attendance.data.model.DeviceLogin
import com.gorilla.attendance.data.model.DeviceLoginData
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class ChooseMemberRepository @Inject constructor(
    private val apiService: ApiService,
    private val deviceLoginDao: DeviceLoginDao
) {

}