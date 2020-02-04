package com.gorilla.attendance.ui.common

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gorilla.attendance.data.model.Acceptances
import com.gorilla.attendance.data.model.BottomFaceResult
import com.gorilla.attendance.data.model.ClockData
import com.gorilla.attendance.data.model.DeviceLoginData
import com.gorilla.attendance.utils.*
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedViewModel @Inject constructor(
    compositeDisposable: CompositeDisposable,
    offlineIdentifyManager: OfflineIdentifyManager,
    preferences: AppPreferences): ViewModel() {

    private var mCompositeDisposable: CompositeDisposable = compositeDisposable
    private var mIdentifyManager: OfflineIdentifyManager = offlineIdentifyManager
    private var mPreferences: AppPreferences = preferences

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

    //var updateLanguageEvent = MutableLiveData<Boolean>()

    var bottomFaceResultEvent = SingleLiveEvent<BottomFaceResult>()

    var userAgreeEvent = SingleLiveEvent<Boolean>()
    var userHadAgreeEvent = SingleLiveEvent<Boolean>()

    var restartAppEvent = SingleLiveEvent<Boolean>()

    var toastEvent = SingleLiveEvent<String>()

    var retrainId = ""
    var isUserProfileExist = false

    fun initSingleIdentifier() {
        // no need now
//        if (clockAcceptances?.intId == null) {
//            Timber.d("int id == null, return")
//            return
//        }
//
//        SimpleRxTask.onIoThread {
//            if (clockModule == MODULE_VISITOR) {
//                mIdentifyManager.initSingleVisitorIdentify(clockAcceptances?.intId!!)
//            } else {
//                mIdentifyManager.initSingleEmployeeIdentify(clockAcceptances?.intId!!)
//            }
//        }
    }

    fun getFullName(): String {
        var fullName = ""

        clockAcceptances?.let {
            fullName = it.lastName + it.firstName
        }

        return fullName
    }

    /**
     * Is current use clock_in/clock_out in verification mode
     */
    fun isOptionClockMode(): Boolean {
        return mPreferences.applicationMode == Constants.VERIFICATION_MODE
                && mPreferences.checkMode == Constants.CHECK_OPTION
    }

    /**
     * If visitor want to do the registration, he/she must agree the user agreement.
     */
    fun isNeedUserAgreement(): Boolean {
        return mPreferences.applicationMode == Constants.REGISTER_MODE
                && clockModule == MODULE_VISITOR
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

    override fun onCleared() {
        super.onCleared()
        mCompositeDisposable.clear()
    }
}