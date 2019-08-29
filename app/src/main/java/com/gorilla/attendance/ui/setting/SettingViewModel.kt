package com.gorilla.attendance.ui.setting

import androidx.lifecycle.ViewModel
import com.gorilla.attendance.utils.Constants
import com.gorilla.attendance.utils.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class SettingViewModel @Inject constructor(
    settingRepository: SettingRepository,
    compositeDisposable: CompositeDisposable
) : ViewModel() {
    private val mSettingRepository: SettingRepository = settingRepository
    private val mCompositeDisposable : CompositeDisposable = compositeDisposable

    var verifyAccountEvent = SingleLiveEvent<Boolean>()

    fun verifyAccount(username: String, password: String) {
        for (account in Constants.settingAccounts) {
            if (account.username == username && account.password == password) {
                verifyAccountEvent.postValue(true)
                return
            }
        }

        verifyAccountEvent.postValue(false)
    }

    override fun onCleared() {
        super.onCleared()
        mCompositeDisposable.clear()
    }
}
