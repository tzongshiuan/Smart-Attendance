package com.gorilla.attendance.ui.main

import android.app.AlertDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import com.gorilla.attendance.data.TestData
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gorilla.attendance.R
import com.gorilla.attendance.data.model.*
import com.gorilla.attendance.ui.common.SharedViewModel
import com.gorilla.attendance.ui.register.RegisterViewModel
import com.gorilla.attendance.ui.screenSaver.ScreenSaverViewModel
import com.gorilla.attendance.utils.*
import com.gorilla.attendance.utils.networkChecker.NetworkChecker
import com.jakewharton.rxbinding.view.RxView
import gorilla.fdr.Identify
import gorilla.fdr.Type
import gorilla.iod.IntelligentObjectDetector
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import io.reactivex.subscribers.DisposableSubscriber
import kotlinx.android.synthetic.main.language_select_dialog.view.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.collections.ArrayList

class MainViewModel @Inject constructor(
    mainRepository: MainRepository,
    sharedViewModel: SharedViewModel,
    registerViewModel: RegisterViewModel,
    screenSaverViewModel: ScreenSaverViewModel,
    preferences: PreferencesHelper,
    compositeDisposable: CompositeDisposable,
    networkChecker: NetworkChecker,
    videoManager: VideoManager,
    offlineIdentifyManager: OfflineIdentifyManager
) : ViewModel() {
    private val mMainRepository: MainRepository = mainRepository
    private var mSharedViewModel: SharedViewModel = sharedViewModel
    private var mRegisterViewModel: RegisterViewModel = registerViewModel
    private var mScreenSaverViewModel: ScreenSaverViewModel = screenSaverViewModel
    private var mPreferences: PreferencesHelper = preferences
    private var mCompositeDisposable: CompositeDisposable = compositeDisposable
    private var mNetworkChecker: NetworkChecker = networkChecker
    private var mVideoManager: VideoManager = videoManager
    private var mIdentifyManager: OfflineIdentifyManager = offlineIdentifyManager

    companion object {
        const val LANGUAGE_CH_SIM = 0
        const val LANGUAGE_CH_TRA = 1
        const val LANGUAGE_EN = 2
    }

    val initialLoad = MutableLiveData<NetworkState>()

    val deviceLoginData = SingleLiveEvent<DeviceLoginData>()

    val faceVerifyErrorData = SingleLiveEvent<ErrorData>()
    val faceVerifyUiMode = SingleLiveEvent<Int>()

    val userRegisterErrorData = SingleLiveEvent<ErrorData>()
    val userRegisterUiMode = SingleLiveEvent<Int>()

    private var listClockData: Array<ClockData>? = null

    private var clockEventJob: Job? = null
    private var isEnableCoroutine = false
    private val listBufferClockData = ArrayList<ClockData>()

    var isUpdateUser = false
    private var isUpdateRecord = false
    private var isUpdateOther = false
    private var isClearData = false

    // toolbar
    val dateTimeData = SingleLiveEvent<ArrayList<String>>()

    val languageIdChangeEvent = SingleLiveEvent<Int>()

    private var updateUserCountDown = 0
    private var updateRecordCountDown = 0
    private var updateOtherCountDown = 0
    private var clearDataCountDown = 0

    /**
     * Composite four APIs, deviceLogin + getDeviceEmployees + getDeviceVisitors + getDeviceIdentities
     */
    fun deviceInit(deviceToken: String?, deviceType: String?, deviceIp: String?) {
        // to check whether the current device had downloaded identities before
        val disposableObserver = object : DisposableSingleObserver<List<AttendanceDeviceInfoData>>() {
            override fun onSuccess(value: List<AttendanceDeviceInfoData>) {
                Timber.d("List<AttendanceDeviceInfoData> size = ${value.size}")

                if (value.isNotEmpty()) {
                    val data = value[0]
                    // simple choose the first entity, and update clockData
                    Timber.d("[deviceInit] find match info, to get the update identities, updateTime: ${data.updateTime}")
                    deviceInit(deviceToken, deviceType, deviceIp, data.updateTime)
                } else {
                    Timber.d("[deviceInit] device info data not found, to get the whole identities")
                    deviceInit(deviceToken, deviceType, deviceIp, null)
                }
            }

            override fun onError(e: Throwable) {
                Timber.d("getDeviceInfoData onError e = $e")
                initialLoad.postValue(NetworkState.error(e.message))
            }
        }

        mCompositeDisposable.add(mMainRepository.getDeviceInfoData(deviceToken)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribeWith(disposableObserver))
    }

    private fun deviceInit(deviceToken: String?, deviceType: String?, deviceIp: String?, updateTime: String?) {
        initialLoad.postValue(NetworkState.LOADING)
        val disposableObserver = object: DisposableObserver<DeviceLogin>() {
            override fun onNext(value: DeviceLogin) {
                Timber.d("deviceInit onNext value = $value")

                deviceLoginData.postValue(value.data)
            }

            override fun onError(e: Throwable) {
                Timber.d("deviceInit onError e = $e")
                initialLoad.postValue(NetworkState.error("Device initialization failed."))
            }

            override fun onComplete() {
                Timber.d("deviceInit onComplete")
                initialLoad.postValue(NetworkState.LOADED)
            }
        }

        mCompositeDisposable.add(mMainRepository.deviceInit(deviceToken, deviceType, deviceIp, updateTime)
            .observeOn(Schedulers.io())
            .subscribeWith(disposableObserver))
    }

    /**
     * Offline mode related to [deviceInit]
     */
    fun deviceInitializedFromDb(deviceToken : String?) {
        initialLoad.postValue(NetworkState.LOADING)

        val disposableObserver = object: DisposableSubscriber<DeviceLoginData>() {
            override fun onNext(value: DeviceLoginData) {
                Timber.d("deviceInitializedFromDb token = ${value.deviceToken}")
                Timber.d("deviceInitFromDb onNext value.deviceName = ${value.deviceName}")
                Timber.d("deviceInitFromDb onNext value.modulesModes = ${value.modulesModes}")
                Timber.d("deviceInitFromDb onNext value.modulesModes.size = ${value.modulesModes?.size}")
                Timber.d("deviceInitFromDb onNext value.modulesModes?.get(0)?.module = ${value.modulesModes?.get(0)?.module}")
                Timber.d("deviceInitFromDb onNext value.modulesModes?.get(0)?.modes = ${value.modulesModes?.get(0)?.modes}")

                deviceLoginData.postValue(value)
                initialLoad.postValue(NetworkState.LOADED)
            }

            override fun onError(e: Throwable) {
                Timber.d("deviceInitializedFromDb onError e = $e")
                initialLoad.postValue(NetworkState.error("Device initialization from DB failed."))
            }

            override fun onComplete() {
                Timber.d("deviceInitializedFromDb onComplete")
            }
        }

        mCompositeDisposable.add(mMainRepository.deviceInitFromDb(deviceToken)
            .observeOn(Schedulers.io())
            .subscribeWith(disposableObserver))
    }

    fun deleteAllAcceptance() {
        mMainRepository.deleteAllAcceptance()
    }

    fun getDeviceEmployees(deviceToken : String?) {
        val disposableObserver = object : DisposableObserver<DeviceEmployees>() {
            override fun onNext(value: DeviceEmployees) {
                //Timber.d("getDeviceEmployees onNext value = $value")
            }

            override fun onError(e: Throwable) {
                Timber.d("getDeviceEmployees onError e = $e")
            }

            override fun onComplete() {
                //Timber.d("getDeviceEmployees onComplete")
            }
        }

        mCompositeDisposable.add(mMainRepository.getDeviceEmployees(deviceToken)
            .observeOn(Schedulers.io())
            .subscribeWith(disposableObserver))
    }

    fun getDeviceVisitors(deviceToken: String?) {
        val disposableObserver = object : DisposableObserver<DeviceVisitors>() {
            override fun onNext(value: DeviceVisitors) {
                //Timber.d("getDeviceVisitors onNext value = $value")
            }

            override fun onError(e: Throwable) {
                Timber.d("getDeviceVisitors onError e = $e")
            }

            override fun onComplete() {
                //Timber.d("getDeviceVisitors onComplete")
            }
        }

        mCompositeDisposable.add(mMainRepository.getDeviceVisitors(deviceToken)
            .observeOn(Schedulers.io())
            .subscribeWith(disposableObserver))
    }

    fun getDeviceIdentities(deviceToken : String?) {
        //Timber.d("getDeviceIdentities()")

        // to check whether the current device had downloaded identities before
        val disposableObserver = object : DisposableSingleObserver<List<AttendanceDeviceInfoData>>() {
            override fun onSuccess(value: List<AttendanceDeviceInfoData>) {
                //Timber.d("List<AttendanceDeviceInfoData> size = ${value.size}")

                if (value.isNotEmpty()) {
                    // simple choose the first entity, and update clockData
                    val data = value[0]
                    Timber.d("[getDeviceIdentities] find match info, to get the update identities, updateTime: ${data.updateTime}")
                    getDeviceIdentitiesAfterTime(deviceToken, data.updateTime)
                } else {
                    Timber.d("[getDeviceIdentities] device info data not found, to get the whole identities")
                    getDeviceIdentitiesAll(deviceToken)
                }
            }

            override fun onError(e: Throwable) {
                Timber.d("getDeviceInfoData onError e = $e")
                initialLoad.postValue(NetworkState.error(e.message))

                isUpdateUser = false
            }
        }

        mCompositeDisposable.add(mMainRepository.getDeviceInfoData(deviceToken)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribeWith(disposableObserver))
    }

    private fun getDeviceIdentitiesAll(deviceToken : String?) {
        Timber.d("getDeviceIdentitiesAll()")

        val disposableObserver = object : DisposableObserver<DeviceIdentities>() {
            override fun onNext(value: DeviceIdentities) {
                Timber.d("getDeviceIdentities onNext value = $value")
            }

            override fun onError(e: Throwable) {
                Timber.d("getDeviceIdentities onError e = $e")
            }

            override fun onComplete() {
                Timber.d("getDeviceIdentities onComplete")

                isUpdateUser = false
            }
        }

        mCompositeDisposable.add(mMainRepository.getDeviceIdentities(deviceToken)
            .observeOn(Schedulers.io())
            .subscribeWith(disposableObserver))
    }

    private fun getDeviceIdentitiesAfterTime(deviceToken : String?, updateTime: String?) {
        //Timber.d("getDeviceIdentitiesAfterTime(), updateTime: $updateTime")

        val disposableObserver = object : DisposableObserver<DeviceIdentities>() {
            override fun onNext(identities: DeviceIdentities) {
                //Timber.d("getDeviceIdentities onNext value = identities")

                if (identities.data?.employees?.size != 0 || identities.data?.visitors?.size != 0) {
                    Timber.d("Detect device identities had changed.")
                    mSharedViewModel.toastEvent.postValue(Constants.UPDATE_USER_SUCCESS_MAGIC)
                }
            }

            override fun onError(e: Throwable) {
                Timber.d("getDeviceIdentities onError e = $e")
            }

            override fun onComplete() {
                //Timber.d("getDeviceIdentities onComplete")

                isUpdateUser = false
            }
        }

        mCompositeDisposable.add(mMainRepository.getDeviceIdentitiesAfterTime(deviceToken, updateTime)
            .observeOn(Schedulers.io())
            .subscribeWith(disposableObserver))
    }

    fun getDeviceMarquees(deviceToken : String?) {
        val disposableObserver = object : DisposableObserver<DeviceMarquees>() {
            override fun onNext(value: DeviceMarquees) {
                //Timber.d("getDeviceMarquees onNext value = $value")
            }

            override fun onError(e: Throwable) {
                Timber.d("getDeviceMarquees onError e = $e")
            }

            override fun onComplete() {
                //Timber.d("getDeviceMarquees onComplete")
                mScreenSaverViewModel.syncMarqueesEvent.postValue(true)
            }
        }

        mCompositeDisposable.add(mMainRepository.getDeviceMarquees(deviceToken)
            .observeOn(Schedulers.io())
            .subscribeWith(disposableObserver))
    }

    fun getDeviceMarqueesFromDB(deviceToken : String?) {
        val disposableObserver = object: DisposableSubscriber<DeviceMarqueesData>() {
            override fun onNext(value: DeviceMarqueesData) {
                //Timber.d("getDeviceMarqueesFromDB onNext value = $value")

                DeviceUtils.deviceMarquees = value.marquees
            }

            override fun onError(e: Throwable) {
                Timber.d("getDeviceMarqueesFromDB onError e = $e")
            }

            override fun onComplete() {
                //Timber.d("getDeviceMarqueesFromDB onComplete")
            }
        }

        mCompositeDisposable.add(mMainRepository.getDeviceMarqueesFromDB(deviceToken)
            .observeOn(Schedulers.io())
            .subscribeWith(disposableObserver))
    }

    fun getDeviceVideos(deviceToken: String?, isFromWebSocket: Boolean = false) {
        isUpdateOther = true

        val disposableObserver = object : DisposableObserver<DeviceVideos>() {
            override fun onNext(value: DeviceVideos) {
                //Timber.d("getDeviceVideos onNext value = $value")

                if (isFromWebSocket) {
                    mScreenSaverViewModel.stopVideoEvent.postValue(true)
                    mVideoManager.updateAllVideos(mSharedViewModel, mScreenSaverViewModel)
                } else {
                    mVideoManager.downloadEmptyVideo(mSharedViewModel, mScreenSaverViewModel)
                }

                mScreenSaverViewModel.syncVideosEvent.postValue(true)
            }

            override fun onError(e: Throwable) {
                Timber.d("getDeviceVideos onError e = $e")
                isUpdateOther = false
            }

            override fun onComplete() {
                //Timber.d("getDeviceVideos onComplete")
                isUpdateOther = false
            }
        }

        mCompositeDisposable.add(mMainRepository.getDeviceVideos(deviceToken)
            .observeOn(Schedulers.io())
            .subscribeWith(disposableObserver))
    }

    fun getDeviceVideosFromDB(deviceToken: String?) {
        val disposableObserver = object: DisposableSubscriber<DeviceVideosData>() {
            override fun onNext(value: DeviceVideosData) {
                //Timber.d("getDeviceVideosFromDB onNext value = $value")

                DeviceUtils.deviceVideos = value.videos
            }

            override fun onError(e: Throwable) {
                Timber.d("getDeviceVideosFromDB onError e = $e")
            }

            override fun onComplete() {
                //Timber.d("getDeviceVideosFromDB onComplete")
            }
        }

        mCompositeDisposable.add(mMainRepository.getDeviceVideosFromDB(deviceToken)
            .observeOn(Schedulers.io())
            .subscribeWith(disposableObserver))
    }

    private fun saveClockDataToDB(clockData: ClockData?) {
        val disposableSubscriber = object : DisposableSubscriber<Long>() {
            override fun onNext(value: Long) {
                Timber.d("saveClockDataToDB onNext value = $value")

                // reset common clock data after save success
                mSharedViewModel.clockData = ClockData()
                mSharedViewModel.clockAcceptances = null
                DeviceUtils.mFacePngList.clear()
            }

            override fun onError(e: Throwable) {
                Timber.d("saveClockDataToDB onError e = $e")
            }

            override fun onComplete() {
                Timber.d("saveClockDataToDB onComplete")
            }
        }

        mCompositeDisposable.add(mMainRepository.saveClockData(clockData)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(disposableSubscriber))
    }

    /**
     * Save the clock data added while sending[clockAttendance] into DB
     */
    private fun saveClockDataWhileSending() {
        val array = arrayOfNulls<ClockData>(listBufferClockData.size)
        listBufferClockData.toArray(array)
        for (clockData in array){
            saveClockDataToDB(clockData)
        }

        listBufferClockData.clear()
        isUpdateRecord = false
    }

    /**
     * Since app start, if network is enabled => send clock event to server periodically
     */
    private fun getClockDataFromDB() {
        //Timber.d("getClockDataFromDB()")
        val disposableSubscriber = object : DisposableSingleObserver<Array<ClockData>>() {
            override fun onSuccess(value: Array<ClockData>) {
                //Timber.d("getClockDataFromDB onSuccess value.size = ${value.size}")
                listClockData = value
                //clearClockDataFromDB()
                clockAttendance()
            }

            override fun onError(e: Throwable) {
                Timber.d("getClockDataFromDB onError e = $e")
                isUpdateRecord = false
            }
        }

        mCompositeDisposable.add(mMainRepository.getClockData()
            .observeOn(Schedulers.io())
            .subscribeWith(disposableSubscriber))
    }

    private fun clearClockDataFromDB() {
        Timber.d("clearClockDataFromDB")

        listClockData?.let {
            val disposableSubscriber = object : DisposableSingleObserver<Int>() {
                override fun onSuccess(value: Int) {
                    Timber.d("clearClockDataFromDB onSuccess value.size = $value")
                    saveClockDataWhileSending()
                }

                override fun onError(e: Throwable) {
                    Timber.d("clearClockDataFromDB onError e = $e")
                    saveClockDataWhileSending()
                }
            }

            mCompositeDisposable.add(
                mMainRepository.clearClockData(it)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(disposableSubscriber)
            )
        }
    }

    private fun clockAttendance() {
        //Timber.d("clockAttendance()")

        if (listClockData?.size == 0) {
            Timber.d("There are no clock data in database.")
            isUpdateRecord = false
            return
        }

        if (!mNetworkChecker.isNetworkAvailable()) {
            Timber.d("Network is offline, do nothing")
            isUpdateRecord = false
            return
        }

        listClockData?.let {
            val disposableSubscriber = object : DisposableObserver<Boolean>() {
                override fun onNext(isSuccess: Boolean) {
                    Timber.d("clockAttendance isSuccess: $isSuccess")

                    clearClockDataFromDB()

                    if (!isSuccess) {
                        Timber.e("Some api must get failed, check log messages.")
                    }
                }

                override fun onError(e: Throwable) {
                    Timber.d("clockAttendance onError e = $e")
                    saveClockDataWhileSending()
                }

                override fun onComplete() {
                    //Timber.d("clockAttendance onComplete")
                }
            }

            mCompositeDisposable.add(
                mMainRepository.clockAttendance(it)
                    .observeOn(Schedulers.io())
                    .subscribeWith(disposableSubscriber)
            )
        }
    }

    /**
     * Save different types of clock event to Database when user activate clock event.
     */
    fun sendSuccessClockEvent(clockType: Int?) {
        Timber.d("sendSuccessClockEvent(), clockType: $clockType")

        mSharedViewModel.clockData.serial = mPreferences.clockSerialNumber++
        mSharedViewModel.clockData.deviceToken = mPreferences.tabletToken

        /**
         * Already set in previous procedures
         */
        //mSharedViewModel.clockData.securityCode = "07121234"
        //mSharedViewModel.clockData.liveness = Constants.LIVENESS_SUCCEED

        mSharedViewModel.clockData.id = mSharedViewModel.clockAcceptances?.id
        mSharedViewModel.clockData.deviceTime = DateUtils.nowDateTime2Str()

        mSharedViewModel.clockData.faceVerify = Constants.FACE_VERIFY_SUCCEED
        if (MainActivity.IS_SKIP_FDR) {
            mSharedViewModel.clockData.faceImg = TestData.FACE_IMAGE
        } else {
            if (DeviceUtils.mFacePngList.size > 0) {
                mSharedViewModel.clockData.faceImg =
                    Base64.encodeToString(DeviceUtils.mFacePngList[0], Base64.DEFAULT)
            } else {
                mSharedViewModel.clockData.faceImg = "Image Lost"
            }
        }
        mSharedViewModel.clockData.mode = mSharedViewModel.clockMode
        mSharedViewModel.clockData.recordMode = Constants.RECORD_MODE_RECORD

        mSharedViewModel.clockData.firstName = mSharedViewModel.clockAcceptances?.firstName
        mSharedViewModel.clockData.lastName = mSharedViewModel.clockAcceptances?.lastName
        mSharedViewModel.clockData.intId = mSharedViewModel.clockAcceptances?.intId ?: 0

        mSharedViewModel.clockData.clockType = clockType
        when (clockType) {
            FaceVerifyResultView.CLOCK_CHECK_IN -> mSharedViewModel.clockData.type = "IN"
            FaceVerifyResultView.CLOCK_CHECK_OUT -> mSharedViewModel.clockData.type = "OUT"
            FaceVerifyResultView.CLOCK_PASS -> mSharedViewModel.clockData.type = "IN"   // follow document
            FaceVerifyResultView.CLOCK_ARRIVE -> mSharedViewModel.clockData.type = "IN"
            FaceVerifyResultView.CLOCK_LEAVE -> mSharedViewModel.clockData.type = "OUT"
            FaceVerifyResultView.CLOCK_TIMEOUT -> {}
        }

        // Save clock event into DB
        if (isUpdateRecord) {
            Timber.d("Sending clock now...save data to temporary list")
            listBufferClockData.add(ClockData(mSharedViewModel.clockData))
        } else {
            saveClockDataToDB(mSharedViewModel.clockData)
        }

        showClockDataInfo()
    }

    /**
     * Save any verification failed event (unrecognizedLog API)
     */
    fun sendFailedClockEvent() {
        Timber.d("sendFailedClockEvent()")

        mSharedViewModel.clockData.serial = mPreferences.clockSerialNumber++
        mSharedViewModel.clockData.deviceToken = mPreferences.tabletToken
        mSharedViewModel.clockData.deviceTime = DateUtils.nowDateTime2Str()
        if (MainActivity.IS_SKIP_FDR) {
            mSharedViewModel.clockData.faceImg = TestData.FACE_IMAGE
        } else {
            if (DeviceUtils.mFacePngList.size != 0) {
                mSharedViewModel.clockData.faceImg = Base64.encodeToString(DeviceUtils.mFacePngList[0], Base64.DEFAULT)
            } else {
                mSharedViewModel.clockData.faceImg = ""
            }
        }
        mSharedViewModel.clockData.mode = mSharedViewModel.clockMode
        mSharedViewModel.clockData.recordMode = Constants.RECORD_MODE_UNRECOGNIZED
        mSharedViewModel.clockData.clockType = if (mSharedViewModel.clockModule == SharedViewModel.MODULE_VISITOR)
            FaceVerifyResultView.CLOCK_ARRIVE else FaceVerifyResultView.CLOCK_CHECK_IN
        mSharedViewModel.clockData.rfid = ""    // temporary set empty string

        // Save clock event into DB
        if (isUpdateRecord) {
            Timber.d("Sending clock now...save data to temporary list")
            listBufferClockData.add(ClockData(mSharedViewModel.clockData))
        } else {
            saveClockDataToDB(mSharedViewModel.clockData)
        }

        showClockDataInfo()
    }

    fun restartSingleMode() {
        if (mSharedViewModel.isSingleModuleMode()) {
            mSharedViewModel.restartSingleModeEvent.postValue(mSharedViewModel.clockMode)
        }
    }

    private fun showClockDataInfo() {
        Timber.i("======================================================================")
        Timber.i("clockData.serial: ${mSharedViewModel.clockData.serial}")
        Timber.i("clockData.deviceToken: ${mSharedViewModel.clockData.deviceToken}")
        Timber.i("clockData.id: ${mSharedViewModel.clockData.id}")
        Timber.i("clockData.securityCode: ${mSharedViewModel.clockData.securityCode}")
        Timber.i("clockData.deviceTime: ${mSharedViewModel.clockData.deviceTime}")
        Timber.i("clockData.liveness: ${mSharedViewModel.clockData.liveness}")
        Timber.i("clockData.faceVerify: ${mSharedViewModel.clockData.faceVerify}")
        Timber.i("clockData.mode: ${mSharedViewModel.clockData.mode}")
        Timber.i("clockData.type: ${mSharedViewModel.clockData.type}")
        Timber.i("clockData.recordMode: ${mSharedViewModel.clockData.recordMode}")
        Timber.i("clockData.firstName: ${mSharedViewModel.clockData.firstName}")
        Timber.i("clockData.lastName: ${mSharedViewModel.clockData.lastName}")
        Timber.i("clockData.intId: ${mSharedViewModel.clockData.intId}")
        Timber.i("======================================================================")
    }

    fun resetUpdateCounter() {
        synchronized(updateUserCountDown) {
            updateUserCountDown = mPreferences.updateUserTime
            updateRecordCountDown = mPreferences.updateRecordTime
            updateOtherCountDown = mPreferences.updateOtherTime
            clearDataCountDown = mPreferences.clearDataTime * 60
            Timber.d("restartUpdateCounter(), user: %s, record: %s, other: %s, clear: %s",
                updateOtherCountDown, updateRecordCountDown, updateOtherCountDown, clearDataCountDown)
        }
    }

    fun enableCoroutine() {
        resetUpdateCounter()
        isEnableCoroutine = true
    }

    fun disableCoroutine() {
        resetUpdateCounter()
        isEnableCoroutine = false
    }

    /**
     * Start the clock event sending coroutine which is sending clock event saved in DB to the server in every periodical time.
     */
    fun startClockEventSendingJob() = GlobalScope.launch(Dispatchers.IO) {
        Timber.d("startClockEventSendingJob()")

        clockEventJob = launch(Dispatchers.IO) {
            try {
                while (true) {
                    // control update cycle behaviors here
                    delay(DeviceUtils.TIMER_DELAYED_TIME)

                    if (!isEnableCoroutine) {
                        continue
                    }

                    synchronized(updateUserCountDown) {
                        if (!isUpdateUser) {
                            updateUserCountDown--
                        }

                        if (!isUpdateRecord) {
                            updateRecordCountDown--
                        }

                        if (!isUpdateOther) {
                            updateOtherCountDown--
                        }

                        if (!isClearData) {
                            clearDataCountDown--
                        }

                        // Sync employees, visitors and identities
                        if (updateUserCountDown <= 0) {
                            if (!isUpdateUser) {
                                isUpdateUser = true
                                Timber.d("[UPDATE CYCLE] Update User")

                                if (mNetworkChecker.isNetworkAvailable()) {
                                    //deleteAllAcceptance()
                                    getDeviceEmployees(mPreferences.tabletToken)
                                    getDeviceVisitors(mPreferences.tabletToken)
                                    getDeviceIdentities(mPreferences.tabletToken)
                                } else {
                                    Timber.d("Network is offline, do nothing")
                                    isUpdateUser = false
                                }
                            } else {
                                Timber.d("isUpdateUser = true, sync user info from server...")
                            }

                            updateUserCountDown = mPreferences.updateUserTime
                        }

                        // Sync clock records
                        if (updateRecordCountDown <= 0) {
                            if (!isUpdateRecord) {
                                isUpdateRecord = true
                                Timber.d("[UPDATE CYCLE] Update Record")
                                getClockDataFromDB()
                            } else {
                                Timber.d("isUpdateRecord = true, Sending clock data to server...")
                            }

                            updateRecordCountDown = mPreferences.updateRecordTime
                        }

                        // Sync marquees, videos
                        if (updateOtherCountDown <= 0) {
                            if (mVideoManager.isDownloading) {
                                Timber.d("VideoManager is downloading...")
                            } else {
                                if (!isUpdateOther) {
                                    isUpdateOther = true
                                    Timber.d("[UPDATE CYCLE] Update Other")

                                    if (mNetworkChecker.isNetworkAvailable()) {
                                        getDeviceMarquees(mPreferences.tabletToken)
                                        getDeviceVideos(mPreferences.tabletToken)
                                    } else {
                                        Timber.d("Network is offline, do nothing")
                                        isUpdateOther = false
                                    }
                                } else {
                                    Timber.d("isUpdateOther = true, sync others data from server...")
                                }
                            }

                            updateOtherCountDown = mPreferences.updateOtherTime
                        }

                        // clear local data, currently will clear face images
                        if (clearDataCountDown <= 0) {
                            if (!isClearData) {
                                isClearData = true
                                Timber.d("[UPDATE CYCLE] Clear Data")
                                SimpleRxTask.onIoThread {
                                    clearLocalFaceImages()
                                }
                            } else {
                                Timber.d("isClearData = true, clear data in the local folder")
                            }

                            clearDataCountDown = mPreferences.clearDataTime * 60
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.d("cancelled clockEventJob, $e")
            }
        }
    }

    private fun clearLocalFaceImages() {
        try {
            val dir = File(DeviceUtils.SD_CARD_APP_FACE_IMAGE)
            if (dir.exists()) {
                val files = dir.listFiles()
                for (file in files) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
        isClearData = false
    }

    /**
     * Stop the clock event sending thread
     */
    fun stopClockEventSendingJob() = runBlocking {
        Timber.d("stopClockEventSendingJob()")
        clockEventJob?.cancel()
    }

    fun updateDateTime(isImmediate: Boolean = false) {
        if (isImmediate) {
            val listResult = ArrayList<String>()
            val now = Calendar.getInstance().time

            // Time
            var sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            listResult.add(sdf.format(now))

            // Date
            sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            listResult.add(sdf.format(now))

            // Day of week
            sdf = SimpleDateFormat("EEE", Locale.getDefault())
            listResult.add(sdf.format(now))

            dateTimeData.postValue(listResult)
        } else {
            Completable.complete()
                .delay(DeviceUtils.TIMER_DELAYED_TIME, TimeUnit.MILLISECONDS)
                .doOnComplete {
                    //Timber.d("Update date time")

                    val listResult = ArrayList<String>()
                    val now = Calendar.getInstance().time

                    // Time
                    var sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    listResult.add(sdf.format(now))

                    // Date
                    sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    listResult.add(sdf.format(now))

                    // Day of week
                    sdf = SimpleDateFormat("EEE", Locale.getDefault())
                    listResult.add(sdf.format(now))

                    dateTimeData.postValue(listResult)

                    // continue to update date time
                    updateDateTime()

                    // TODO customize with different locale
//                if (DeviceUtils.mLocale != null) {
//                    when {
//                        DeviceUtils.mLocale.equals(Constants.LOCALE_EN) -> {
//                            sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
//                        }
//                        DeviceUtils.mLocale.equals(Constants.LOCALE_TW) -> {
//                            sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
//                        }
//                        DeviceUtils.mLocale.equals(Constants.LOCALE_CN) -> {
//                            sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
//                        }
//                        else -> sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
//                    }
//                }
                }
                .subscribe()
        }
    }

    /*******************************************************************************************************************
     * About face verification
     */
    fun onGetFaceSuccess() {
        ImageUtils.saveImages(DeviceUtils.mFacePngList.size, DeviceUtils.mFacePngList, "face-success")
        mSharedViewModel.clockData.faceVerify = Constants.FACE_VERIFY_SUCCEED

        Timber.i("sharedViewModel.clockModule= ${mSharedViewModel.clockModule}")
        Timber.i("sharedViewModel.clockMode = ${mSharedViewModel.clockMode}")

        if (MainActivity.IS_SKIP_FDR) {
            DeviceUtils.mFacePngList.add(TestData.FACE_BYTE_ARRAY)
        }

        if (mPreferences.applicationMode == Constants.REGISTER_MODE) {
            initialLoad.value = NetworkState.LOADING

            // Registration
            if (mNetworkChecker.isNetworkAvailable()) {
                userRegistration()
            } else {
                Timber.d("Network is unavailable for registration")
                userRegisterErrorData.postValue(ErrorData().also {
                    it.code = "network_unavailable"
                    it.message = "Please check network"
                })
                onReceiveRegisterResult(Constants.USER_REGISTER_FAILED)
            }
        } else {
            // Verification
            if (mPreferences.isFdrOnlineMode && mNetworkChecker.isNetworkAvailable()) {
                // Do online verification (through REFTful API)
                onlineFaceVerification()
            } else {
                // Do offline verification (through ROOM database)
                SimpleRxTask.onIoThread {
                    offlineFaceVerification()
                }
            }
        }
    }

    fun onGetFaceFailed() {
        ImageUtils.saveImages(DeviceUtils.mFacePngList.size, DeviceUtils.mFacePngList, "face-fail")
        mSharedViewModel.clockData.faceVerify = Constants.FACE_VERIFY_FAILED
    }

    private fun userRegistration() {
        if (mSharedViewModel.isUserProfileExist) {
            // Retrain face image
            when (mSharedViewModel.clockModule) {
                SharedViewModel.MODULE_VISITOR -> visitorRetrain()
                else -> employeeRetrain()
            }
        } else {
            // Register new user
            when (mSharedViewModel.clockModule) {
                SharedViewModel.MODULE_VISITOR -> visitorRegistration()
                else -> employeeRegistration()
            }
        }
    }

    /**
     * Do retrain for employee
     */
    private fun employeeRetrain() {
        Timber.d("employeeRetrain()")

        val disposableObserver = object: DisposableObserver<ClockResponse>() {
            override fun onNext(response: ClockResponse) {
                Timber.d("status: ${response.status}")

                if (response.status == ApiResponseModel.STATUS_SUCCESS) {
                    Timber.d("employeeRetrain() success")
                    onReceiveRegisterResult(Constants.USER_REGISTER_SUCCESS)
                } else {
                    Timber.d("employeeRetrain() failed")
                    userRegisterErrorData.postValue(response.error)
                    onReceiveRegisterResult(Constants.USER_REGISTER_FAILED)
                }
            }

            override fun onError(e: Throwable) {
                Timber.d("employeeRetrain(), onError e = $e")
                initialLoad.postValue(NetworkState.error(e.message))
            }

            override fun onComplete() {
                Timber.d("employeeRetrain(), onComplete")
                initialLoad.postValue(NetworkState.LOADED)
            }
        }

        val observable = mMainRepository.retrainUser(
            mSharedViewModel.retrainId, Constants.USER_TYPE_EMPLOYEE,
            Constants.IMAGE_FORMAT_PNG, DeviceUtils.mFacePngList[0]
        )

        mCompositeDisposable.add(observable
            .subscribeWith(disposableObserver))
    }

    /**
     * Do retrain for visitor
     */
    private fun visitorRetrain() {
        Timber.d("visitorRetrain()")

        val disposableObserver = object: DisposableObserver<ClockResponse>() {
            override fun onNext(response: ClockResponse) {
                Timber.d("status: ${response.status}")

                if (response.status == ApiResponseModel.STATUS_SUCCESS) {
                    Timber.d("visitorRetrain() success")
                    onReceiveRegisterResult(Constants.USER_REGISTER_SUCCESS)
                } else {
                    Timber.d("visitorRetrain() failed")
                    userRegisterErrorData.postValue(response.error)
                    onReceiveRegisterResult(Constants.USER_REGISTER_FAILED)
                }
            }

            override fun onError(e: Throwable) {
                Timber.d("visitorRetrain(), onError e = $e")
                initialLoad.postValue(NetworkState.error(e.message))
            }

            override fun onComplete() {
                Timber.d("visitorRetrain(), onComplete")
                initialLoad.postValue(NetworkState.LOADED)
            }
        }

        val observable = mMainRepository.retrainUser(
            mSharedViewModel.retrainId, Constants.USER_TYPE_VISITOR,
            Constants.IMAGE_FORMAT_PNG, DeviceUtils.mFacePngList[0]
        )

        mCompositeDisposable.add(observable
            .subscribeWith(disposableObserver))
    }

    /**
     * Do employee registration
     */
    private fun employeeRegistration() {
        Timber.d("employeeRegistration()")

        val disposableObserver = object: DisposableObserver<RegisterUserResponse>() {
            override fun onNext(response: RegisterUserResponse) {
                Timber.d("status: ${response.status}")

                if (response.status == ApiResponseModel.STATUS_SUCCESS) {
                    Timber.d("employeeRegistration() success")
                    onReceiveRegisterResult(Constants.USER_REGISTER_SUCCESS)
                } else {
                    Timber.d("employeeRegistration() failed")
                    userRegisterErrorData.postValue(response.error)
                    onReceiveRegisterResult(Constants.USER_REGISTER_FAILED)
                }
            }

            override fun onError(e: Throwable) {
                Timber.d("employeeRegistration(), onError e = $e")
                initialLoad.postValue(NetworkState.error(e.message))
            }

            override fun onComplete() {
                Timber.d("employeeRegistration(), onComplete")
                initialLoad.postValue(NetworkState.LOADED)
            }
        }

        mRegisterViewModel.employeeRegisterData?.let {
            val observable = mMainRepository.registerEmployee(
                it, Constants.IMAGE_FORMAT_PNG, DeviceUtils.mFacePngList[0]
            )

            mCompositeDisposable.add(observable
                .subscribeWith(disposableObserver))
        }
    }

    /**
     * Do visitor registration
     */
    private fun visitorRegistration() {
        Timber.d("visitorRegistration()")

        val disposableObserver = object: DisposableObserver<RegisterUserResponse>() {
            override fun onNext(response: RegisterUserResponse) {
                Timber.d("status: ${response.status}")

                if (response.status == ApiResponseModel.STATUS_SUCCESS) {
                    Timber.d("visitorRegistration() success")
                    onReceiveRegisterResult(Constants.USER_REGISTER_SUCCESS)
                } else {
                    Timber.d("visitorRegistration() failed")
                    userRegisterErrorData.postValue(response.error)
                    onReceiveRegisterResult(Constants.USER_REGISTER_FAILED)
                }
            }

            override fun onError(e: Throwable) {
                Timber.d("visitorRegistration(), onError e = $e")
                initialLoad.postValue(NetworkState.error(e.message))
            }

            override fun onComplete() {
                Timber.d("visitorRegistration(), onComplete")
                initialLoad.postValue(NetworkState.LOADED)
            }
        }

        mRegisterViewModel.visitorRegisterData?.let {
            val observable = mMainRepository.registerVisitor(
                it, Constants.IMAGE_FORMAT_PNG, DeviceUtils.mFacePngList[0]
            )

            mCompositeDisposable.add(observable
                .subscribeWith(disposableObserver))
        }
    }

    /**
     * Online verification
     */
    private fun onlineFaceVerification() {
        Timber.d("onlineFaceVerification()")

        val disposableObserver = object : DisposableObserver<ClockResponse>() {
            override fun onNext(response: ClockResponse) {
                Timber.d("status: ${response.status}")

                if (mSharedViewModel.clockMode == SharedViewModel.MODE_FACE_IDENTIFICATION) {
                    /**
                     * SharedViewModel.MODE_FACE_IDENTIFICATION
                     */
                    if (response.status == ApiResponseModel.STATUS_SUCCESS) {
                        Timber.d("bap identification success")
                        val acceptance = response.data
                        mSharedViewModel.clockAcceptances = acceptance
                        mSharedViewModel.clockData.securityCode = acceptance?.securityCode

                        onReceiveFdrResult(Constants.FDR_VERIFY_SUCCESS)
                    } else {
                        Timber.d("bap identification failed")
                        faceVerifyErrorData.postValue(response.error)
                        onReceiveFdrResult(Constants.FDR_VERIFY_FAILED)
                    }
                } else {
                    if (response.status == ApiResponseModel.STATUS_SUCCESS) {
                        Timber.d("bap verify success")
                        onReceiveFdrResult(Constants.FDR_VERIFY_SUCCESS)
                    } else {
                        Timber.d("bap verify failed")
                        faceVerifyErrorData.postValue(response.error)
                        onReceiveFdrResult(Constants.FDR_VERIFY_FAILED)
                    }
                }
            }

            override fun onError(e: Throwable) {
                Timber.d("onlineFaceVerification(), onError e = $e")
                initialLoad.value = NetworkState.error(e.message)
            }

            override fun onComplete() {
                Timber.d("onlineFaceVerification(), onComplete")
                initialLoad.value = NetworkState.LOADED
            }
        }

        val observable = if (mSharedViewModel.clockMode == SharedViewModel.MODE_FACE_IDENTIFICATION) {
            Timber.d("Get face success, go through \"Identify\" path")

            if (mSharedViewModel.clockModule == SharedViewModel.MODULE_VISITOR) {
                mMainRepository.onlineFaceIdentify(
                    Constants.USER_TYPE_VISITOR,
                    Constants.IMAGE_FORMAT_PNG,
                    DeviceUtils.mFacePngList[0]
                )
            } else {
                mMainRepository.onlineFaceIdentify(
                    Constants.USER_TYPE_EMPLOYEE,
                    Constants.IMAGE_FORMAT_PNG,
                    DeviceUtils.mFacePngList[0]
                )
            }
        } else {
            Timber.d("Get face success, go through \"Verify\" path")

            if (mSharedViewModel.clockModule == SharedViewModel.MODULE_VISITOR) {
                mMainRepository.onlineFaceVerify(
                    mSharedViewModel.clockAcceptances?.id!!,
                    Constants.USER_TYPE_VISITOR,
                    Constants.IMAGE_FORMAT_PNG,
                    DeviceUtils.mFacePngList[0]
                )
            } else {
                mMainRepository.onlineFaceVerify(
                    mSharedViewModel.clockAcceptances?.id!!,
                    Constants.USER_TYPE_EMPLOYEE,
                    Constants.IMAGE_FORMAT_PNG,
                    DeviceUtils.mFacePngList[0]
                )
            }
        }

        mCompositeDisposable.add(observable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(disposableObserver))
    }

    /**
     * Offline verification
     */
    private fun offlineFaceVerification() {
        Timber.d("offlineFaceVerification()")

        val faceList = arrayOfNulls<Type.ONEFACE>(DeviceUtils.mFacePngList.size)
        getFaceList(DeviceUtils.mFacePngList, faceList)

        val candidateList = arrayOfNulls<Identify.CANDIDATE>(DeviceUtils.VERIFIED_CANDIDATE_NUMBER)
        for (i in candidateList.indices) {
            candidateList[i] = Identify.CANDIDATE()
        }

        val decision = BooleanArray(1)
        // Check DB for safety
        if (mSharedViewModel.clockModule == SharedViewModel.MODULE_VISITOR) {
            if (mIdentifyManager.mIdentifyVisitors == null) {
                Timber.d("visitor identify == null")
                initialLoad.value = NetworkState.error("No visitor identities in the DB")
                return
            }

            if (mSharedViewModel.clockAcceptances?.intId != null) {
//                while (mIdentifyManager.isInitSingleIdentifier) {
//                    Timber.d("still init visitor single identify")
//                    Thread.sleep(1000)
//                }
//                mIdentifyManager.mSingleVisitorIdentify?.search(faceList, candidateList, decision)

                val array = IntArray(1)
                array[0] = mSharedViewModel.clockAcceptances?.intId!!
                mIdentifyManager.mIdentifyVisitors?.identifyActiveID(1, array)
                mIdentifyManager.mIdentifyVisitors?.search(
                    faceList, mSharedViewModel.clockAcceptances?.intId!!, candidateList, decision
                )
            } else {
                mIdentifyManager.mIdentifyVisitors?.search(
                    faceList, candidateList, decision
                )
            }
        } else {
            if (mIdentifyManager.mIdentifyEmployees == null) {
                Timber.d("employee identify == null")
                initialLoad.value = NetworkState.error("No employee identities in the DB")
                return
            }

            if (mSharedViewModel.clockMode != SharedViewModel.MODE_FACE_IDENTIFICATION
                && mSharedViewModel.clockAcceptances?.intId != null) {
//                while (mIdentifyManager.isInitSingleIdentifier) {
//                    Timber.d("still init employee single identify")
//                    Thread.sleep(300)
//                }
//                mIdentifyManager.mSingleEmployeeIdentify?.search(faceList, candidateList, decision)

                val array = IntArray(1)
                array[0] = mSharedViewModel.clockAcceptances?.intId!!
                mIdentifyManager.mIdentifyEmployees?.identifyActiveID(1, array)
                mIdentifyManager.mIdentifyEmployees?.search(
                    faceList, mSharedViewModel.clockAcceptances?.intId!!, candidateList, decision
                )
            } else {
                mIdentifyManager.mIdentifyEmployees?.search(
                    faceList, candidateList, decision
                )
            }
        }

        Timber.d("candidateList.length = %s", candidateList.size)
        Timber.d("candidateList[0].valid = %s", candidateList[0]?.valid)
        Timber.d("candidateList[0].similarity_score = %f", candidateList[0]?.similiarity_score)
        Timber.d("decision[0] = %s", decision[0])
        Timber.d("candidateList[0].template_id = %s", candidateList[0]?.template_id)
        Timber.d("candidateList find int id = %d", mSharedViewModel.clockAcceptances?.intId)

        if (decision[0]) {
            // verify success
            // Use id to find the corresponding acceptance
            if (mSharedViewModel.clockMode == SharedViewModel.MODE_FACE_IDENTIFICATION
                && candidateList[0]?.template_id != null) {
                getAcceptanceFromIntId(candidateList[0]?.template_id!!)
            } else {
//                if (mSharedViewModel.clockAcceptances?.intId == candidateList[0]?.template_id) {
                    Timber.d("Offline face verification success")
                    onReceiveFdrResult(Constants.FDR_VERIFY_SUCCESS)
                    initialLoad.postValue(NetworkState.LOADED)
//                } else {
//                    Timber.d("Offline face verification failed, unmatch intId !!")
//                    onReceiveFdrResult(Constants.FDR_VERIFY_FAILED)
//                    initialLoad.value = NetworkState.LOADED
//                }
            }
        } else {
            // verify failed
            Timber.d("Offline face verification failed")
            onReceiveFdrResult(Constants.FDR_VERIFY_FAILED)
            initialLoad.postValue(NetworkState.LOADED)
        }
    }

    private fun getFaceList(pngList: List<ByteArray>, faceList: Array<Type.ONEFACE?>) {
        //File root = Environment.getExternalStorageDirectory();
        //File file = new File(root, "/Download/Module/NobelHsu_0040.jpg");
        //Bitmap myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        for (i in pngList.indices) {
            Timber.d("i = $i, pngList[i].size = ${pngList[i].size}")
            val myBitmap = BitmapFactory.decodeByteArray(pngList[i], 0, pngList[i].size)
            val size = myBitmap.rowBytes * myBitmap.height

            val byteBuffer = ByteBuffer.allocate(size)
            myBitmap.copyPixelsToBuffer(byteBuffer)
            val byteArray = byteBuffer.array()
            IntelligentObjectDetector.convertRGBAtoBGRA(byteArray, myBitmap.width, myBitmap.height, myBitmap.rowBytes)

            faceList[i] = Type.ONEFACE()
            faceList[i]?.image_width = myBitmap.width
            faceList[i]?.image_height = myBitmap.height
            faceList[i]?.image_step = myBitmap.rowBytes
            faceList[i]?.image_nchannel = myBitmap.rowBytes / myBitmap.width
            faceList[i]?.roi_x = 0
            faceList[i]?.roi_y = 0
            faceList[i]?.roi_width = myBitmap.width
            faceList[i]?.roi_height = myBitmap.height
            faceList[i]?.image_data = byteArray
        }
    }

    private fun getAcceptanceFromIntId(intId: Int) {
        Timber.d("getAcceptanceFromIntId(), intId: $intId")

        val disposableObserver = object : DisposableSingleObserver<List<Acceptances>>() {
            override fun onSuccess(value: List<Acceptances>) {
                Timber.d("getAcceptanceFromIntId() success, size = ${value.size}")

                if (value.isNotEmpty()) {
                    mSharedViewModel.clockAcceptances = value[0]
                    onReceiveFdrResult(Constants.FDR_VERIFY_SUCCESS)
                } else {
                    mIdentifyManager.removeEmployee(intId)
                    onReceiveFdrResult(Constants.FDR_VERIFY_FAILED)
                }

                initialLoad.postValue(NetworkState.LOADED)
            }

            override fun onError(e: Throwable) {
                Timber.d("getAcceptanceFromIntId() onError e = $e")
                initialLoad.postValue(NetworkState.error(e.message))
            }
        }

        mCompositeDisposable.add(
            mMainRepository.getAcceptanceFromIntId(intId)
                .subscribeOn(Schedulers.io())
                .subscribeWith(disposableObserver))
    }

    /**
     * Notify FDR result to observers
     */
    fun onReceiveFdrResult(retCode: Int) {
        when (retCode) {
            Constants.FDR_VERIFY_FAILED -> {
                faceVerifyUiMode.postValue(Constants.UI_FACE_UNKNOWN)
            }

            Constants.FDR_VERIFY_SUCCESS -> {
                faceVerifyUiMode.postValue(Constants.UI_FACE_VALID)
            }

            else -> {}
        }
    }

    /**
     * Notify register result to observers
     */
    fun onReceiveRegisterResult(retCode: Int) {
        when (retCode) {

            Constants.USER_REGISTER_SUCCESS -> {
                userRegisterUiMode.postValue(Constants.UI_REGISTER_COMPLETE)
            }

            Constants.USER_REGISTER_FAILED -> {
                userRegisterUiMode.postValue(Constants.UI_REGISTER_FAILED)
            }

            else -> {}
        }
    }
    /**
     * About face verification(END)
     *******************************************************************************************************************/

    fun getRegisterName(): String? {
        return when (mSharedViewModel.clockModule) {
            SharedViewModel.MODULE_VISITOR -> mRegisterViewModel.visitorRegisterData?.name
            else -> mRegisterViewModel.employeeRegisterData?.name
        }
    }

    fun showLanguageSelectDialog(context: Context) {
        Timber.d("showLanguageSelectDialog()")

        val builder = AlertDialog.Builder(context)
        builder.setCancelable(true)

        val view = View.inflate(context, R.layout.language_select_dialog, null)
        builder.setView(view)

        when (mPreferences.languageId) {
            LANGUAGE_CH_SIM -> {
                view.chSimText.setBackgroundResource(R.drawable.language_select_border_choose)
                view.chSimText.setTextColor(context.getColor(R.color.white))
            }
            LANGUAGE_CH_TRA -> {
                view.chTraText.setBackgroundResource(R.drawable.language_select_border_choose)
                view.chTraText.setTextColor(context.getColor(R.color.white))
            }
            else -> {
                view.enText.setBackgroundResource(R.drawable.language_select_border_choose)
                view.enText.setTextColor(context.getColor(R.color.white))
            }
        }

        val dialog = builder.create().also {
            it.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.show()
        }

        RxView.clicks(view.chSimText)
            .debounce(500, TimeUnit.MILLISECONDS)
            .observeOn(rx.android.schedulers.AndroidSchedulers.mainThread())
            .subscribe {
                if (mPreferences.languageId != LANGUAGE_CH_SIM) {
                    Timber.d("[Language Change] Simplified Chinese")
                    changeLanguageConfig(context, LANGUAGE_CH_SIM)
                    languageIdChangeEvent.postValue(LANGUAGE_CH_SIM)
                    dialog.dismiss()

                    showChangeLanguageHint(context)
                }
            }

        RxView.clicks(view.chTraText)
            .debounce(500, TimeUnit.MILLISECONDS)
            .observeOn(rx.android.schedulers.AndroidSchedulers.mainThread())
            .subscribe {
                if (mPreferences.languageId != LANGUAGE_CH_TRA) {
                    Timber.d("[Language Change] Traditional Chinese")
                    changeLanguageConfig(context, LANGUAGE_CH_TRA)
                    languageIdChangeEvent.postValue(LANGUAGE_CH_TRA)
                    dialog.dismiss()

                    showChangeLanguageHint(context)
                }
            }

        RxView.clicks(view.enText)
            .debounce(500, TimeUnit.MILLISECONDS)
            .observeOn(rx.android.schedulers.AndroidSchedulers.mainThread())
            .subscribe {
                if (mPreferences.languageId != LANGUAGE_EN) {
                    Timber.d("[Language Change] English")
                    changeLanguageConfig(context, LANGUAGE_EN)
                    languageIdChangeEvent.postValue(LANGUAGE_EN)
                    dialog.dismiss()

                    showChangeLanguageHint(context)
                }
            }
    }

    private fun showChangeLanguageHint(context: Context) {
        Toast.makeText(context, context.getString(R.string.restart_from_language_hint), Toast.LENGTH_LONG).show()

        SimpleRxTask.after(2000L) {
            mSharedViewModel.restartAppEvent.postValue(true)
        }
    }

    @Suppress("DEPRECATION")
    fun changeLanguageConfig(context: Context, languageId: Int) {
        Timber.d("[Language Change] language id = $languageId")

        val config = context.resources.configuration

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            when (languageId) {
                LANGUAGE_CH_SIM -> config.setLocale(Locale.SIMPLIFIED_CHINESE)
                LANGUAGE_CH_TRA -> config.setLocale(Locale.TRADITIONAL_CHINESE)
                else -> config.setLocale(Locale.ENGLISH)
            }
            when (languageId) {
                LANGUAGE_CH_SIM -> Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
                LANGUAGE_CH_TRA -> Locale.setDefault(Locale.TRADITIONAL_CHINESE)
                else -> Locale.setDefault(Locale.ENGLISH)
            }
            context.createConfigurationContext(config)
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context.applicationContext.resources.updateConfiguration(config, context.resources.displayMetrics)
        } else {
            when (languageId) {
                LANGUAGE_CH_SIM -> config.locale = Locale.SIMPLIFIED_CHINESE
                LANGUAGE_CH_TRA -> config.locale = Locale.TRADITIONAL_CHINESE
                else -> config.locale = Locale.ENGLISH
            }
            when (languageId) {
                LANGUAGE_CH_SIM -> Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
                LANGUAGE_CH_TRA -> Locale.setDefault(Locale.TRADITIONAL_CHINESE)
                else -> Locale.setDefault(Locale.ENGLISH)
            }
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context.applicationContext.resources.updateConfiguration(config, context.resources.displayMetrics)
        }

        //mSharedViewModel.updateLanguageEvent.postValue(true)    // if need to update language immediately
    }

    override fun onCleared() {
        super.onCleared()
        mCompositeDisposable.clear()
    }
}