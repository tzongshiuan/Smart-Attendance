package com.gorilla.attendance.ui.common

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gorilla.attendance.data.model.Acceptances
import com.gorilla.attendance.data.model.ClockData
import com.gorilla.attendance.data.model.DeviceLoginData
import com.gorilla.attendance.utils.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedViewModel : ViewModel {
    private var mCompositeDisposable: CompositeDisposable


    companion object {
        val MODULE_ATTENDANCE = 1
        val MODULE_ACCESS = 2
        val MODULE_VISITOR = 3
        val MODULE_MIX = 4

        val MODE_SECURITY = 1
        val MODE_RFID = 2
        val MODE_QR_CODE = 3
        val MODE_FACE_ICON = 4  // no use anymore
        val MODE_FACE_IDENTIFICATION = 5
        val MODE_SCANNER = 6    // no use now, but see on the portal
    }

    /****************  Clock data ***************************/
    var clockData = ClockData()
    var clockModule: Int = 0
    var clockMode: Int = 0

    var clockAcceptances: Acceptances? = null

    var deviceName: String? = null

    var deviceLoginData = MutableLiveData<DeviceLoginData>()

    var changeTitleEvent = SingleLiveEvent<String>()
    var fdrResultVisibilityEvent = SingleLiveEvent<Int>()

    var restartSingleModeEvent = SingleLiveEvent<Int>()

    var verifyFinishEvent = SingleLiveEvent<Boolean>()
    var registerFinishEvent = SingleLiveEvent<Boolean>()

    var retrainId = ""
    var isUserProfileExist = false

    fun getFullName(): String {
        var fullName = ""

        clockAcceptances?.let {
            fullName = it.lastName + it.firstName
        }

        return fullName
    }

    fun isSingleModuleMode(): Boolean {
        val moduleModes = deviceLoginData.value?.modulesModes

        moduleModes?.let {
            if (it.size == 1 && it[0].modes?.size == 1) {
                return true
            }
        }

        return false
    }

    fun isSingleMode(): Boolean {
        val moduleModes = deviceLoginData.value?.modulesModes

        moduleModes?.let {
            when (clockModule) {
                MODULE_VISITOR -> {
                    for (moduleMode in it) {
                        if (moduleMode.module != MODULE_VISITOR) {
                            continue
                        }

                        if (moduleMode.modes?.size == 1) {
                            return true
                        }
                    }
                }

                else -> {
                    for (moduleMode in it) {
                        if (moduleMode.module == MODULE_VISITOR) {
                            continue
                        }

                        if (moduleMode.modes?.size == 1) {
                            return true
                        }
                    }
                }
            }
        }

        return false
    }

    @Inject
    constructor(compositeDisposable: CompositeDisposable) {
        mCompositeDisposable = compositeDisposable
    }
}