package com.gorilla.attendance.ui.main

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.gorilla.attendance.api.ApiService
import com.gorilla.attendance.data.TestData
import com.gorilla.attendance.data.db.*
import com.gorilla.attendance.data.model.*
import com.gorilla.attendance.utils.*
import com.gorilla.attendance.utils.networkChecker.NetworkChecker
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
import io.reactivex.functions.Function4
import io.reactivex.functions.Function5
import io.reactivex.functions.Function8
import io.reactivex.schedulers.Schedulers
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject

class MainRepository @Inject constructor (
    private val apiService: ApiService,
    private val deviceLoginDao: DeviceLoginDao,
    private val deviceEmployeeDao: DeviceEmployeeDao,
    private val deviceVisitorDao: DeviceVisitorDao,
    private val deviceIdentitiesDao: DeviceIdentitiesDao,
    private val deviceMarqueesDao: DeviceMarqueesDao,
    private val deviceVideosDao: DeviceVideosDao,
    private val clockDao: ClockDao,
    private val preferences: AppPreferences,
    private val mNetworkChecker: NetworkChecker,
    private val mOfflineIdentifyManager: OfflineIdentifyManager
) {

    /**************************************************************
     * about device initialization
     */
    fun deviceInit(deviceToken: String?, deviceType: String?, deviceIp: String?, updateTime: String?): Observable<DeviceLogin> {
        return deviceInitApi(deviceToken, deviceType, deviceIp, updateTime)
    }

    fun deviceInitFromDb(deviceToken: String?): Flowable<DeviceLoginData> {
        return deviceInitFromDbApi(deviceToken)
    }

    fun getDeviceEmployees(deviceToken: String?): Observable<DeviceEmployees> {
        return getDeviceEmployeesApi(deviceToken)
    }

    fun getDeviceVisitors(deviceToken: String?): Observable<DeviceVisitors> {
        return getDeviceVisitorsApi(deviceToken)
    }

    fun getDeviceIdentities(deviceToken: String?): Observable<DeviceIdentities> {
        return getDeviceIdentitiesApi(deviceToken)
    }

    fun getDeviceIdentitiesAfterTime(deviceToken: String?, updateTime: String?): Observable<DeviceIdentities> {
        return getDeviceIdentitiesAfterTimeApi(deviceToken, updateTime)
    }

    fun getDeviceMarquees(deviceToken: String?): Observable<DeviceMarquees> {
        return getDeviceMarqueesApi(deviceToken)
    }

    fun getDeviceMarqueesFromDB(deviceToken: String?): Flowable<DeviceMarqueesData> {
        return getDeviceMarqueesFromDbApi(deviceToken)
    }

    fun getDeviceVideos(deviceToken: String?): Observable<DeviceVideos> {
        return getDeviceVideosApi(deviceToken)
    }

    fun getDeviceVideosFromDB(deviceToken: String?): Flowable<DeviceVideosData> {
        return getDeviceVideosFromDbApi(deviceToken)
    }

    fun getDeviceInfoData(deviceToken: String?): Single<List<AttendanceDeviceInfoData>> {
        return deviceIdentitiesDao.searchDeviceByDeviceToken(deviceToken)
    }

    fun getAcceptanceFromIntId(intId: Int): Single<List<Acceptances>> {
        return deviceEmployeeDao.searchDeviceEmployeeByIntId(intId)
    }
    //**************************************************************





    /**************************************************************
     * about clock data
     */
    fun saveClockData(clockData: ClockData?): Flowable<Long> {
        return Flowable.fromCallable { clockDao.insertClockData(clockData) }.subscribeOn(Schedulers.io())
    }

    fun getClockData(): Single<Array<ClockData>> {
        return clockDao.getClockData().subscribeOn(Schedulers.io())
    }

    fun clearClockData(listClockData: Array<ClockData>): Single<Int> {
        return Single.fromCallable { clockDao.deleteClockData(listClockData) }.subscribeOn(Schedulers.io())
    }

    fun clockAttendance(listClockData: Array<ClockData>): Observable<Boolean> {
        return clockAttendanceApi(listClockData)
    }
    //**************************************************************





    /**************************************************************
     * about registration
     */
    fun registerEmployee(body: EmployeeRegisterData, imageFormat: String, imageData: ByteArray): Observable<RegisterUserResponse> {
        return registerEmployeeApi(body, imageFormat, imageData)
    }

    fun registerVisitor(body: VisitorRegisterData, imageFormat: String, imageData: ByteArray): Observable<RegisterUserResponse> {
        return registerVisitorApi(body, imageFormat, imageData)
    }
    //**************************************************************





    /**************************************************************
     * about verification
     */
    fun onlineFaceIdentify(type: String, imageFormat: String, imageData: ByteArray): Observable<ClockResponse> {
        return bapIdentifyApi(type, imageFormat, imageData)
    }

    fun onlineFaceVerify(id: String, type: String, imageFormat: String, imageData: ByteArray): Observable<ClockResponse> {
        return bapVerifyApi(id, type, imageFormat, imageData)
    }

    fun retrainUser(id: String, type: String, imageFormat: String, imageData: ByteArray): Observable<ClockResponse> {
        return bapEnrollApi(id, type, imageFormat, imageData)
    }
    //**************************************************************





    private fun deviceInitApi(deviceToken: String?, deviceType: String?, deviceIp: String?, updateTime: String?): Observable<DeviceLogin> {
        val deviceLoginOb = apiService.deviceLogin(deviceToken, deviceType, deviceIp)
        val getDeviceEmployeesOb = apiService.getDeviceEmployees(deviceToken)
        val getDeviceVisitorsOb = apiService.getDeviceVisitors(deviceToken)
        val getDeviceIdentitiesOb = apiService.getDeviceIdentities(deviceToken)
        val getUpdateIdentitiesOb = apiService.getUpdateDeviceIdentities(deviceToken, updateTime)
        val deviceIdentitiesFlow = deviceIdentitiesDao.getDeviceIdentitiesData(deviceToken).toObservable()

        if (mNetworkChecker.isNetworkAvailable()) {
            if (updateTime == null) {
                return Observable.zip<Response<DeviceLogin>, Response<DeviceEmployees>, Response<DeviceVisitors>
                        , Response<DeviceIdentities>, DeviceLogin>(
                    deviceLoginOb, getDeviceEmployeesOb, getDeviceVisitorsOb, getDeviceIdentitiesOb,
                    Function4 { loginResponse, employeeResponse
                                , visitorResponse, identitiesResponse ->
                        var isSuccess = true
                        if (loginResponse.isSuccessful && loginResponse.body()?.status == ApiResponseModel.STATUS_SUCCESS) {
                            Timber.d("loginResponse success")
                            loginResponse.body()?.data?.deviceToken = deviceToken
                            deviceLoginDao.insertDeviceLoginData(loginResponse.body()?.data)

                            // delete old acceptances
                            deviceLoginDao.deleteAcceptances()
                        } else {
                            isSuccess = false
                        }

                        if (employeeResponse.isSuccessful && employeeResponse.body()?.status == ApiResponseModel.STATUS_SUCCESS) {
                            Timber.d("employeeResponse success")
                            Timber.d(
                                "employeeResponse?.body()?.data?.acceptances?.size = %s",
                                employeeResponse.body()?.data?.acceptances?.size
                            )
                            val acceptances = employeeResponse.body()?.data?.acceptances
                            acceptances?.let {
                                for (acceptance in it) {
                                    acceptance.deviceToken = preferences.tabletToken
                                    acceptance.type = Constants.USER_TYPE_EMPLOYEE
                                    deviceEmployeeDao.insertDeviceEmployeeData(acceptance)
                                }
                            }
                        } else {
                            isSuccess = false
                        }

                        if (visitorResponse.isSuccessful && visitorResponse.body()?.status == ApiResponseModel.STATUS_SUCCESS) {
                            Timber.d("visitorResponse success")
                            Timber.d(
                                "visitorResponse?.body()?.data?.acceptances?.size = %s",
                                visitorResponse.body()?.data?.acceptances?.size
                            )
                            val acceptances = visitorResponse.body()?.data?.acceptances
                            acceptances?.let {
                                for (acceptance in it) {
                                    acceptance.deviceToken = preferences.tabletToken
                                    acceptance.type = Constants.USER_TYPE_VISITOR
                                    deviceVisitorDao.insertDeviceVisitorData(acceptance)
                                }
                            }
                        } else {
                            isSuccess = false
                        }

                        if (identitiesResponse.isSuccessful && identitiesResponse.body()?.status == ApiResponseModel.STATUS_SUCCESS) {
                            Timber.d("identitiesResponse success")
                            val identities = identitiesResponse.body()?.data
                            identities?.deviceToken = preferences.tabletToken

                            // Initial mIdentify
                            mOfflineIdentifyManager.initWithIdentities(identities)

                            // Save identities into DB for offline verification
                            deviceIdentitiesDao.insertDeviceIdentitiesData(identities)

                            // Update device info data in the database
                            val deviceInfoData = AttendanceDeviceInfoData().also {
                                it.serverIp = preferences.serverIp
                                it.deviceToken = deviceToken
                                it.updateTime = DateUtils.nowDateTimeForIdentities()
                            }
                            Timber.d("AA updateTime: ${deviceInfoData.updateTime}")
                            deviceIdentitiesDao.insertDeviceInfoData(deviceInfoData)
                        } else {
                            isSuccess = false
                        }

                        if (isSuccess && loginResponse.body() != null) {
                            loginResponse.body()!!
                        } else {
                            val error = Gson().fromJson(loginResponse.errorBody()?.string(), DeviceLogin::class.java)
                            error
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
            } else {
                return Observable.zip<Response<DeviceLogin>, Response<DeviceEmployees>, Response<DeviceVisitors>
                        , Response<DeviceIdentities>, DeviceIdentitiesData, DeviceLogin>(
                    deviceLoginOb, getDeviceEmployeesOb, getDeviceVisitorsOb, getUpdateIdentitiesOb, deviceIdentitiesFlow,
                    Function5 { loginResponse, employeeResponse
                                , visitorResponse, identitiesResponse, preIdentitiesData ->
                        var isSuccess = true
                        if (loginResponse.isSuccessful && loginResponse.body()?.status == ApiResponseModel.STATUS_SUCCESS) {
                            Timber.d("loginResponse success")
                            loginResponse.body()?.data?.deviceToken = deviceToken
                            deviceLoginDao.insertDeviceLoginData(loginResponse.body()?.data)

                            // delete old acceptances
                            deviceLoginDao.deleteAcceptances()
                        } else {
                            isSuccess = false
                        }

                        if (employeeResponse.isSuccessful && employeeResponse.body()?.status == ApiResponseModel.STATUS_SUCCESS) {
                            Timber.d("employeeResponse success")
                            Timber.d(
                                "employeeResponse?.body()?.data?.acceptances?.size = %s",
                                employeeResponse.body()?.data?.acceptances?.size
                            )
                            val acceptances = employeeResponse.body()?.data?.acceptances
                            acceptances?.let {
                                for (acceptance in it) {
                                    acceptance.deviceToken = preferences.tabletToken
                                    acceptance.type = Constants.USER_TYPE_EMPLOYEE
                                    deviceEmployeeDao.insertDeviceEmployeeData(acceptance)
                                }
                            }
                        } else {
                            isSuccess = false
                        }

                        if (visitorResponse.isSuccessful && visitorResponse.body()?.status == ApiResponseModel.STATUS_SUCCESS) {
                            Timber.d("visitorResponse success")
                            Timber.d(
                                "visitorResponse?.body()?.data?.acceptances?.size = %s",
                                visitorResponse.body()?.data?.acceptances?.size
                            )
                            val acceptances = visitorResponse.body()?.data?.acceptances
                            acceptances?.let {
                                for (acceptance in it) {
                                    acceptance.deviceToken = preferences.tabletToken
                                    acceptance.type = Constants.USER_TYPE_VISITOR
                                    deviceVisitorDao.insertDeviceVisitorData(acceptance)
                                }
                            }
                        } else {
                            isSuccess = false
                        }

                        if (identitiesResponse.isSuccessful && identitiesResponse.body()?.status == ApiResponseModel.STATUS_SUCCESS) {
                            Timber.d("identitiesResponse success")
                            val identities = identitiesResponse.body()?.data
                            identities?.deviceToken = preferences.tabletToken

                            Timber.d("preIdentitiesData.employees?.size = ${preIdentitiesData.employees?.size}")
                            Timber.d("preIdentitiesData.visitors?.size = ${preIdentitiesData.visitors?.size}")
                            mOfflineIdentifyManager.initWithIdentities(preIdentitiesData)

                            if (identities?.employees?.size != 0 || identities.visitors?.size != 0) {
                                identities?.employees?.let {
//                                    for (employee in it) {
//                                        preIdentitiesData.employees?.let { preEmployees ->
//                                            if (!preEmployees.contains(employee)) {
//                                                preEmployees.add(employee)
//                                            } else {
//                                                preEmployees.set(preEmployees.indexOf(employee), employee)
//                                            }
//                                        }
//                                    }
                                    mOfflineIdentifyManager.insertEmployees(it)
                                    preIdentitiesData.employees = mOfflineIdentifyManager.employeeList
                                }
                                identities?.visitors?.let {
//                                    for (visitor in it) {
//                                        preIdentitiesData.visitors?.let { preVisitors ->
//                                            if (!preVisitors.contains(visitor)) {
//                                                preVisitors.add(visitor)
//                                            } else {
//                                                preVisitors.set(preVisitors.indexOf(visitor), visitor)
//                                            }
//                                        }
//                                    }
                                    mOfflineIdentifyManager.insertVisitors(it)
                                    preIdentitiesData.visitors = mOfflineIdentifyManager.visitorList
                                }
                                // Save identities into DB for offline verification
                                deviceIdentitiesDao.insertDeviceIdentitiesData(preIdentitiesData)
                            }

                            // Update device info data in the database
                            val deviceInfoData = AttendanceDeviceInfoData().also {
                                it.serverIp = preferences.serverIp
                                it.deviceToken = deviceToken
                                it.updateTime = identities?.updateTime
                            }
                            Timber.d("BB updateTime: ${deviceInfoData.updateTime}")
                            deviceIdentitiesDao.insertDeviceInfoData(deviceInfoData)
                        } else {
                            isSuccess = false
                        }

                        if (isSuccess && loginResponse.body() != null) {
                            loginResponse.body()!!
                        } else {
                            val error = Gson().fromJson(loginResponse.errorBody()?.string(), DeviceLogin::class.java)
                            error
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
            }
        } else {
            return Observable.zip<Response<DeviceLogin>, Response<DeviceEmployees>, Response<DeviceVisitors>, DeviceLogin>(
                deviceLoginOb, getDeviceEmployeesOb, getDeviceVisitorsOb,
                Function3 { loginResponse, employeeResponse, visitorResponse ->
                    var isSuccess = true
                    if (loginResponse.isSuccessful && loginResponse.body()?.status == ApiResponseModel.STATUS_SUCCESS) {
                        Timber.d("loginResponse success")
                        loginResponse.body()?.data?.deviceToken = deviceToken
                        deviceLoginDao.insertDeviceLoginData(loginResponse.body()?.data)
                    } else {
                        isSuccess = false
                    }

                    if (employeeResponse.isSuccessful && employeeResponse.body()?.status == ApiResponseModel.STATUS_SUCCESS) {
                        Timber.d("employeeResponse success")
                        Timber.d("employeeResponse?.body()?.data?.acceptances?.size = %s", employeeResponse.body()?.data?.acceptances?.size)
                        val acceptances = employeeResponse.body()?.data?.acceptances
                        acceptances?.let {
                            for (acceptance in it) {
                                acceptance.deviceToken = preferences.tabletToken
                                deviceEmployeeDao.insertDeviceEmployeeData(acceptance)
                            }
                        }
                    } else {
                        isSuccess = false
                    }

                    if (visitorResponse.isSuccessful && visitorResponse.body()?.status == ApiResponseModel.STATUS_SUCCESS) {
                        Timber.d("visitorResponse success")
                        Timber.d("visitorResponse?.body()?.data?.acceptances?.size = %s", visitorResponse.body()?.data?.acceptances?.size)
                        val acceptances = visitorResponse.body()?.data?.acceptances
                        acceptances?.let {
                            for (acceptance in it) {
                                acceptance.deviceToken = preferences.tabletToken
                                deviceVisitorDao.insertDeviceVisitorData(acceptance)
                            }
                        }
                    } else {
                        isSuccess = false
                    }

                    if (isSuccess && loginResponse.body() != null) {
                        loginResponse.body()!!
                    } else {
                        val error = Gson().fromJson(loginResponse.errorBody()?.string(), DeviceLogin::class.java)
                        error
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
        }
    }

    private fun deviceInitFromDbApi(deviceToken: String?): Flowable<DeviceLoginData> {
        val deviceLoginFlow = deviceLoginDao.getDeviceLoginData(deviceToken)
        val deviceIdentitiesFlow = deviceIdentitiesDao.getDeviceIdentitiesData(deviceToken)

        return Flowable.zip<DeviceLoginData, DeviceIdentitiesData, DeviceLoginData>(deviceLoginFlow, deviceIdentitiesFlow,
            BiFunction<DeviceLoginData, DeviceIdentitiesData, DeviceLoginData> {
                    deviceLoginData, deviceIdentitiesData ->

                mOfflineIdentifyManager.initWithPreIdentities(deviceIdentitiesData)
                deviceLoginData
            })
            .subscribeOn(Schedulers.io())
    }

    fun deleteAllAcceptance() {
        SimpleRxTask.onIoThread {
            // delete old acceptances
            deviceLoginDao.deleteAcceptances()
        }
    }

    private fun getDeviceEmployeesApi(deviceToken: String?): Observable<DeviceEmployees> {
        return apiService.getDeviceEmployees(deviceToken)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .map { getDeviceEmployeesResponse ->
                Timber.d("getDeviceEmployeesResponse?.body()?.data?.acceptances?.size = %s", getDeviceEmployeesResponse.body()?.data?.acceptances?.size)
                if (getDeviceEmployeesResponse.isSuccessful) {
                    // delete old employees
                    deviceLoginDao.deleteEmployeeAcceptances(Constants.USER_TYPE_EMPLOYEE)

                    // save to DB
                    val acceptances = getDeviceEmployeesResponse.body()?.data?.acceptances
                    acceptances?.let {
                        for (acceptance in it) {
                            acceptance.deviceToken = preferences.tabletToken
                            acceptance.type = Constants.USER_TYPE_EMPLOYEE
                            deviceEmployeeDao.insertDeviceEmployeeData(acceptance)
                        }
                    }
                    getDeviceEmployeesResponse.body()
                } else {
                    val error = Gson().fromJson(getDeviceEmployeesResponse.errorBody()?.string(), DeviceEmployees::class.java)
                    error
                }
            }
    }

    private fun getDeviceVisitorsApi(deviceToken: String?): Observable<DeviceVisitors> {
        return apiService.getDeviceVisitors(deviceToken)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .map { getDeviceVisitorsResponse ->
                Timber.d("getDeviceVisitorsResponse?.body()?.data?.acceptances?.size = %s", getDeviceVisitorsResponse.body()?.data?.acceptances?.size)
                if (getDeviceVisitorsResponse.isSuccessful) {
                    // delete old employees
                    deviceLoginDao.deleteVisitorAcceptances(Constants.USER_TYPE_VISITOR)

                    // save to DB
                    val acceptances = getDeviceVisitorsResponse.body()?.data?.acceptances
                    acceptances?.let {
                        for (acceptance in it) {
                            acceptance.deviceToken = preferences.tabletToken
                            acceptance.type = Constants.USER_TYPE_VISITOR
                            deviceVisitorDao.insertDeviceVisitorData(acceptance)
                        }
                    }
                    getDeviceVisitorsResponse.body()
                } else {
                    val error =
                        Gson().fromJson(getDeviceVisitorsResponse.errorBody()?.string(), DeviceVisitors::class.java)
                    error
                }
            }
    }

    private fun getDeviceIdentitiesApi(deviceToken: String?): Observable<DeviceIdentities> {
        return apiService.getDeviceIdentities(deviceToken)
            .subscribeOn(Schedulers.io())
            .map { getDeviceIdentitiesResponse ->
                Timber.d("getDeviceIdentitiesApi() success")
                if (getDeviceIdentitiesResponse.isSuccessful) {
                    val identities = getDeviceIdentitiesResponse.body()?.data
                    identities?.deviceToken = preferences.tabletToken

                    // Initial mIdentify
                    mOfflineIdentifyManager.initWithIdentities(identities)

                    // Save identities into DB for offline verification
                    deviceIdentitiesDao.insertDeviceIdentitiesData(identities)

                    // Update device info data in the database
                    val deviceInfoData = AttendanceDeviceInfoData().also {
                        it.serverIp = preferences.serverIp
                        it.deviceToken = deviceToken
                        it.updateTime = DateUtils.nowDateTimeForIdentities()
                    }
                    deviceIdentitiesDao.insertDeviceInfoData(deviceInfoData)

                    getDeviceIdentitiesResponse.body()
                } else {
                    val error =
                        Gson().fromJson(getDeviceIdentitiesResponse.errorBody()?.string(), DeviceIdentities::class.java)
                    error
                }
            }
    }

    private fun getDeviceIdentitiesAfterTimeApi(deviceToken: String?, updateTime: String?): Observable<DeviceIdentities> {
        val getUpdateIdentitiesOb = apiService.getUpdateDeviceIdentities(deviceToken, updateTime)
        val deviceIdentitiesFlow = deviceIdentitiesDao.getDeviceIdentitiesData(deviceToken).toObservable()

        return Observable.zip(
            getUpdateIdentitiesOb, deviceIdentitiesFlow,
            BiFunction<Response<DeviceIdentities>, DeviceIdentitiesData, DeviceIdentities> {
                    identitiesResponse, preIdentitiesData ->
                Timber.d("getDeviceIdentitiesAfterTimeApi() success")
                if (identitiesResponse.isSuccessful && identitiesResponse.body() != null) {
                    val identities = identitiesResponse.body()?.data
                    identities?.deviceToken = preferences.tabletToken

                    Timber.d("preIdentitiesData.employees?.size = ${preIdentitiesData.employees?.size}")
                    Timber.d("preIdentitiesData.visitors?.size = ${preIdentitiesData.visitors?.size}")

                    if (identities?.employees?.size != 0 || identities.visitors?.size != 0) {
                        identities?.employees?.let {
                            mOfflineIdentifyManager.insertEmployees(it)
                            preIdentitiesData.employees = mOfflineIdentifyManager.employeeList
                        }
                        identities?.visitors?.let {
                            for (visitor in it) {
                                preIdentitiesData.visitors?.let { preVisitors ->
                                    if (!preVisitors.contains(visitor)) {
                                        preVisitors.add(visitor)
                                    } else {
                                        preVisitors.set(preVisitors.indexOf(visitor), visitor)
                                    }
                                }
                            }
                            mOfflineIdentifyManager.insertVisitors(it)
                            preIdentitiesData.visitors = mOfflineIdentifyManager.visitorList
                        }
                        // Save identities into DB for offline verification
                        deviceIdentitiesDao.insertDeviceIdentitiesData(preIdentitiesData)
                    }

                    // Update device info data in the database
                    val deviceInfoData = AttendanceDeviceInfoData().also {
                        it.serverIp = preferences.serverIp
                        it.deviceToken = deviceToken
                        it.updateTime = identities?.updateTime
                    }
                    deviceIdentitiesDao.insertDeviceInfoData(deviceInfoData)
                    identitiesResponse.body()!!
                } else {
                    val error =
                        Gson().fromJson(identitiesResponse.errorBody()?.string(), DeviceIdentities::class.java)
                    error
                }
            })
            .subscribeOn(Schedulers.io())
    }

    private fun getDeviceMarqueesApi(deviceToken: String?): Observable<DeviceMarquees> {
        return apiService.getDeviceMarquees(deviceToken)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .map { getDeviceMarqueesResponse ->
                Timber.d("getDeviceMarquees size = %s", getDeviceMarqueesResponse.body()?.data?.marquees?.size)

                if (getDeviceMarqueesResponse.isSuccessful) {
                    val marqueesData = getDeviceMarqueesResponse.body()?.data
                    marqueesData?.deviceToken = preferences.tabletToken

                    // Init marquees
                    DeviceUtils.deviceMarquees = marqueesData?.marquees

                    // Save to DB
                    deviceMarqueesDao.deleteMarqueesData()
                    deviceMarqueesDao.insertDeviceMarqueesData(marqueesData)

                    getDeviceMarqueesResponse.body()
                } else {
                    val error = Gson().fromJson(getDeviceMarqueesResponse.errorBody()?.string(), DeviceMarquees::class.java)
                    error
                }
            }
    }

    private fun getDeviceMarqueesFromDbApi(deviceToken: String?): Flowable<DeviceMarqueesData> {
        return deviceMarqueesDao.getDeviceMarqueesData(deviceToken)
            .subscribeOn(Schedulers.io())
    }

    private fun getDeviceVideosApi(deviceToken: String?): Observable<DeviceVideos> {
        return apiService.getDeviceVideos(deviceToken)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .map { getDeviceVideosResponse ->
                Timber.d("getDeviceVideos size = %s", getDeviceVideosResponse.body()?.data?.videos?.size)

                if (getDeviceVideosResponse.isSuccessful) {
                    val videosData = getDeviceVideosResponse.body()?.data
                    videosData?.deviceToken = preferences.tabletToken
                    videosData?.videos?.sortWith(compareBy({it.priority}, {it.fileSize}))

                    // Init videos
                    DeviceUtils.deviceVideos = videosData?.videos

                    // Save to DB
                    deviceVideosDao.deleteVideosData()
                    deviceVideosDao.insertDeviceVideosData(videosData)

                    getDeviceVideosResponse.body()
                } else {
                    val error = Gson().fromJson(getDeviceVideosResponse.errorBody()?.string(), DeviceVideos::class.java)
                    error
                }
            }
    }

    private fun getDeviceVideosFromDbApi(deviceToken: String?): Flowable<DeviceVideosData> {
        return deviceVideosDao.getDeviceVideosData(deviceToken)
            .subscribeOn(Schedulers.io())
    }

    private fun clockAttendanceApi(listClockData: Array<ClockData>): Observable<Boolean> {
        /********** these data are all from DB******************/
        // safety value check and classify clock data
        val attendanceRecord = DeviceRecordsData().also { it.deviceToken = preferences.tabletToken }
        val accessRecord = DeviceRecordsData().also { it.deviceToken = preferences.tabletToken }
        val visitorRecord = DeviceRecordsData().also { it.deviceToken = preferences.tabletToken }
        val visitorAccessRecord = DeviceRecordsData().also { it.deviceToken = preferences.tabletToken }

        val attendanceUnrecognized = DeviceUnrecognizedData().also { it.deviceToken = preferences.tabletToken }
        val accessUnrecognized = DeviceUnrecognizedData().also { it.deviceToken = preferences.tabletToken }
        val visitorUnrecognized = DeviceUnrecognizedData().also { it.deviceToken = preferences.tabletToken }
        val visitorAccessUnrecognized = DeviceUnrecognizedData().also { it.deviceToken = preferences.tabletToken }

        for (clockData in listClockData) {
            if (clockData.liveness.isNullOrEmpty()) {
                clockData.liveness = ""
            }

            if (clockData.type.isNullOrEmpty()) {
                clockData.type = "IN"
            }

            if (clockData.securityCode.isNullOrEmpty()) {
                clockData.securityCode = "null"
            }

            //clockData.faceImg = "1212"

            when (clockData.clockType) {
                FaceVerifyResultView.CLOCK_CHECK_IN,
                FaceVerifyResultView.CLOCK_CHECK_OUT -> {
                    if (clockData.recordMode == Constants.RECORD_MODE_RECORD) {

                        attendanceRecord.records.add(clockData)
                        accessRecord.records.add(clockData)
                    } else {
                        attendanceUnrecognized.errorLogs.add(clockData)
                        accessUnrecognized.errorLogs.add(clockData)
                    }
                }
                FaceVerifyResultView.CLOCK_PASS -> {
                    if (clockData.recordMode == Constants.RECORD_MODE_RECORD) {
                        accessRecord.records.add(clockData)
                    } else {
                        Timber.log(Log.ERROR, "Should not happen here right now")
                        accessUnrecognized.errorLogs.add(clockData)
                    }
                }
                FaceVerifyResultView.CLOCK_ARRIVE,
                FaceVerifyResultView.CLOCK_LEAVE -> {
                    if (clockData.recordMode == Constants.RECORD_MODE_RECORD) {
                        visitorRecord.records.add(clockData)
                        visitorAccessRecord.records.add(clockData)
                    } else {
                        visitorUnrecognized.errorLogs.add(clockData)
                        visitorAccessUnrecognized.errorLogs.add(clockData)
                    }
                }
            }
        }

        val attendanceOb = apiService.clockAttendance(attendanceRecord)
        val accessOb = apiService.clockAccess(accessRecord)
        val visitorOb = apiService.clockVisitor(visitorRecord)
        val visitorAccessOb = apiService.clockVisitorAccess(visitorAccessRecord)
        val unrecognizedAttendanceOb = apiService.unrecognizedAttendance(attendanceUnrecognized)
        val unrecognizedAccessOb = apiService.unrecognizedAccess(accessUnrecognized)
        val unrecognizedVisitorOb = apiService.unrecognizedVisitor(visitorUnrecognized)
        val unrecognizedVisitorAccessOb = apiService.unrecognizedVisitorAccess(visitorAccessUnrecognized)

        return Observable.zip<Response<ClockResponse>, Response<ClockResponse>, Response<ClockResponse>, Response<ClockResponse>,
                Response<ClockResponse>, Response<ClockResponse>, Response<ClockResponse>, Response<ClockResponse>, Boolean>(
            attendanceOb, accessOb, visitorOb, visitorAccessOb, unrecognizedAttendanceOb
                    , unrecognizedAccessOb, unrecognizedVisitorOb, unrecognizedVisitorAccessOb,
            Function8 { t1, t2, t3, t4
                        , t5, t6, t7, t8 ->
                var isSuccess = true
                if (t1.isSuccessful) {
                    Timber.d("T1 success")
                } else if (attendanceRecord.records.size != 0) {
                    isSuccess = false
                }

                if (t2.isSuccessful) {
                    Timber.d("T2 success")
                } else if (accessRecord.records.size != 0) {
                    isSuccess = false
                }

                if (t3.isSuccessful) {
                    Timber.d("T3 success")
                } else if (visitorRecord.records.size != 0) {
                    isSuccess = false
                }

                if (t4.isSuccessful) {
                    Timber.d("T4 success")
                } else if (visitorAccessRecord.records.size != 0) {
                    isSuccess = false
                }

                if (t5.isSuccessful) {
                    Timber.d("T5 success")
                } else if (attendanceUnrecognized.errorLogs.size != 0) {
                    isSuccess = false
                }

                if (t6.isSuccessful) {
                    Timber.d("T6 success")
                } else if (accessUnrecognized.errorLogs.size != 0) {
                    isSuccess = false
                }

                if (t7.isSuccessful) {
                    Timber.d("T7 success")
                } else if (visitorUnrecognized.errorLogs.size != 0) {
                    isSuccess = false
                }

                if (t8.isSuccessful) {
                    Timber.d("T8 success")
                } else if (visitorAccessUnrecognized.errorLogs.size != 0) {
                    isSuccess = false
                }

                Timber.d("attendance record list, size: ${attendanceRecord.records.size}")
                Timber.d("access record list, size: ${accessRecord.records.size}")
                Timber.d("visitor record list, size: ${visitorRecord.records.size}")
                Timber.d("visitor access record list, size: ${visitorAccessRecord.records.size}")
                Timber.d("attendance unrecognized list, size: ${attendanceUnrecognized.errorLogs.size}")
                Timber.d("access unrecognized list, size: ${accessUnrecognized.errorLogs.size}")
                Timber.d("visitor unrecognized list, size: ${visitorUnrecognized.errorLogs.size}")
                Timber.d("visitor access unrecognized list, size: ${visitorAccessUnrecognized.errorLogs.size}")

                isSuccess
            })
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())

//        val visitorUnrecognized = DeviceUnrecognizedData().also { it.deviceToken = preferences.tabletToken }
//        for (clockData in listClockData) {
//            if (clockData.liveness.isNullOrEmpty()) {
//                clockData.liveness = ""
//            }
//
//            if (clockData.type.isNullOrEmpty()) {
//                clockData.type = "IN"
//            }
//
//            when (clockData.clockType) {
////                FaceVerifyResultView.CLOCK_CHECK_IN,
////                FaceVerifyResultView.CLOCK_CHECK_OUT -> {
////                    if (clockData.recordMode == Constants.RECORD_MODE_RECORD) {
////                        attendanceRecord.records.add(clockData)
////                        accessRecord.records.add(clockData)
////                    } else {
////                        attendanceUnrecognized.errorLogs.add(clockData)
////                        accessUnrecognized.errorLogs.add(clockData)
////                    }
////                }
////                FaceVerifyResultView.CLOCK_PASS -> {
////                    if (clockData.recordMode == Constants.RECORD_MODE_RECORD) {
////                        accessRecord.records.add(clockData)
////                    } else {
////                        Timber.log(Log.ERROR, "Should not happen here right now")
////                        accessUnrecognized.errorLogs.add(clockData)
////                    }
////                }
//                FaceVerifyResultView.CLOCK_ARRIVE,
//                FaceVerifyResultView.CLOCK_LEAVE -> {
////                    if (clockData.recordMode == Constants.RECORD_MODE_RECORD) {
////                        visitorRecord.records.add(clockData)
////                        visitorAccessRecord.records.add(clockData)
////                    } else {
//                        clockData.faceImg = "123"
//                        visitorUnrecognized.errorLogs.add(clockData)
////                        visitorAccessUnrecognized.errorLogs.add(clockData)
////                    }
//                }
//            }
//        }
//
//        return apiService.unrecognizedVisitor(visitorUnrecognized)
//            .subscribeOn(Schedulers.io())
//            .observeOn(Schedulers.io())
//            .map { clockAttendanceResponse ->
//                Timber.d("clockAttendanceResponse.isSuccessful = " +
//                        "${clockAttendanceResponse.isSuccessful}")
//                clockAttendanceResponse.isSuccessful
////                clockAttendanceResponse.body()
//            }
    }

    private fun registerEmployeeApi(body: EmployeeRegisterData, imageFormat: String, imageData: ByteArray): Observable<RegisterUserResponse> {
        body.also {
            val faceData = FaceImageData().also { data ->
                data.format = imageFormat
                if (MainActivity.USE_TEST_FACE) {
                    data.dataInBase64 = TestData.FACE_IMAGE
                } else {
                    data.dataInBase64 = Base64.encodeToString(imageData, Base64.DEFAULT)
                }
            }
            it.imageList = arrayListOf(faceData)
        }

        return apiService.registerEmployee(body)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .map { response ->
                Timber.d("registerEmployeeApi isSuccessful = ${response.isSuccessful}")

                if (response.isSuccessful) {
                    response.body()
                } else {
                    val error = Gson().fromJson(response.errorBody()?.string(), RegisterUserResponse::class.java)
                    error
                }
            }
    }

    private fun registerVisitorApi(body: VisitorRegisterData, imageFormat: String, imageData: ByteArray): Observable<RegisterUserResponse> {
        body.also {
            val faceData = FaceImageData().also { data ->
                data.format = imageFormat
                if (MainActivity.USE_TEST_FACE) {
                    data.dataInBase64 = TestData.FACE_IMAGE
                } else {
                    data.dataInBase64 = Base64.encodeToString(imageData, Base64.DEFAULT)
                }
            }
            it.imageList = arrayListOf(faceData)
        }

        return apiService.registerVisitor(body)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .map { response ->
                Timber.d("registerVisitorApi isSuccessful = ${response.isSuccessful}")

                if (response.isSuccessful) {
                    response.body()
                } else {
                    val error = Gson().fromJson(response.errorBody()?.string(), RegisterUserResponse::class.java)
                    error
                }
            }
    }

    private fun bapIdentifyApi(type: String, imageFormat: String, imageData: ByteArray): Observable<ClockResponse> {
        Timber.d("type: $type")
        Timber.d("imageFormat: $imageFormat")
        Timber.d("imageData: ${imageData.size}")

        val body = BapIdentifyData().also {
            it.type = type
            val faceData = FaceImageData().also { data ->
                data.format = imageFormat
                if (MainActivity.USE_TEST_FACE) {
                    data.dataInBase64 = TestData.FACE_IMAGE
                } else {
                    data.dataInBase64 = Base64.encodeToString(imageData, Base64.DEFAULT)
                }
            }
            it.imageList = arrayListOf(faceData)
        }

        return apiService.bapIdentify(body)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .map { response ->
                Timber.d("bapVerifyApi isSuccessful = ${response.isSuccessful}")

                if (response.isSuccessful) {
                    response.body()
                } else {
                    val error = Gson().fromJson(response.errorBody()?.string(), ClockResponse::class.java)
                    error
                }
            }
    }

    private fun bapVerifyApi(id: String, type: String, imageFormat: String, imageData: ByteArray): Observable<ClockResponse> {
        //Timber.d("id = $id")

        val body = BapVerifyData().also {
            it.id = id
            it.type = type
            val faceData = FaceImageData().also { data ->
                data.format = imageFormat
                if (MainActivity.USE_TEST_FACE) {
                    data.dataInBase64 = TestData.FACE_IMAGE
                } else {
                    data.dataInBase64 = Base64.encodeToString(imageData, Base64.DEFAULT)
                }
            }
            it.imageList = arrayListOf(faceData)
        }

        return apiService.bapVerify(body)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .map { response ->
                Timber.d("bapVerifyApi isSuccessful = ${response.isSuccessful}")

                if (response.isSuccessful) {
                    response.body()
                } else {
                    val error = Gson().fromJson(response.errorBody()?.string(), ClockResponse::class.java)
                    error
                }
            }
    }

    private fun bapEnrollApi(id: String, type: String, imageFormat: String, imageData: ByteArray): Observable<ClockResponse> {
        val body = BapEnrollData().also {
            it.id = id
            it.type = type
            val faceData = FaceImageData().also { data ->
                data.format = imageFormat
                if (MainActivity.USE_TEST_FACE) {
                    data.dataInBase64 = TestData.FACE_IMAGE
                } else {
                    data.dataInBase64 = Base64.encodeToString(imageData, Base64.DEFAULT)
                }
            }
            it.imageList = arrayListOf(faceData)
        }

        return apiService.bapEnroll(body)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .map { response ->
                Timber.d("bapVerifyApi isSuccessful = ${response.isSuccessful}")

                if (response.isSuccessful) {
                    response.body()
                } else {
                    val error = Gson().fromJson(response.errorBody()?.string(), ClockResponse::class.java)
                    error
                }
            }
    }
}