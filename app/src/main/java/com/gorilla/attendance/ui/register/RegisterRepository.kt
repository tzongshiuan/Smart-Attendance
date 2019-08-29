package com.gorilla.attendance.ui.register

import com.google.gson.Gson
import com.gorilla.attendance.api.ApiService
import com.gorilla.attendance.data.model.CheckUserResponse
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class RegisterRepository @Inject constructor(
    private val apiService: ApiService
) {

    fun checkUser(deviceToken: String?, type: String?, securityCode: String?, rfid: String?): Observable<CheckUserResponse> {
        return apiService.checkUser(deviceToken, type, securityCode, rfid)
            .subscribeOn(Schedulers.io())
            .map { checkUserResponse ->
                Timber.d("checkUser() success")
                if (checkUserResponse.isSuccessful) {
                    checkUserResponse.body()
                } else {
                    val error =
                        Gson().fromJson(checkUserResponse.errorBody()?.string(), CheckUserResponse::class.java)
                    error
                }
            }
    }
}