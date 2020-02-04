package com.gorilla.attendance.utils

import android.content.Context
import android.util.Log
import com.gorilla.attendance.ui.common.FetcherListener
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(val context: Context) : PreferencesHelper {

    companion object {
        private const val APP_VERSION = "version"
        private const val APP_SETTING = "app_setting"

        private const val PREF_OBB_LOADED = "obb_loaded"
        private const val PREF_DEVICE_DB_EXIST = "device_db_exist"
        private const val PREF_CLOCK_SERIAL_NUM = "clock_serial_num"

        private const val PREF_LANGUAGE_ID = "language_id"

        // connection setting
        private const val SETTING_SERVER_IP = "server_ip"
        private const val SETTING_WEB_SOCKET_IP = "web_socket_ip"
        private const val SETTING_FTP_ACCOUNT = "ftp_account"
        private const val SETTING_FTP_PASSWORD = "ftp_password"
        private const val SETTING_TABLET_NAME = "tablet_name"
        private const val SETTING_TABLET_TOKEN = "tablet_token"
        private const val SETTING_IS_LOGIN_FINISH = "is_login_finish"

        // function setting
        private const val SETTING_IDLE_TIME = "idle_time"
        private const val SETTING_HOLD_RESULT_TIME = "hold_result_time"
        private const val SETTING_FACE_VERIFY_TIME = "face_verify_time"
        private const val SETTING_APPLICATION_MODE = "application_mode"
        private const val SETTING_CHECK_MODE = "check_mode"
        private const val SETTING_ENABLE_HUMAN_DETECTION = "enable_human_detection"
        private const val SETTING_ENABLE_FDR_ONLINE_MODE = "enable_fdr_online_mode"
        private const val SETTING_ENABLE_SCREEN_SAVER = "enable_screen_saver"

        // linked setting
        private const val SETTING_WEB_CAM = "web_cam"
        private const val SETTING_WEB_CAM_URL = "web_cam_url"
        private const val SETTING_WEB_CAM_USERNAME = "web_cam_username"
        private const val SETTING_WEB_CAM_PASSOWRD = "web_cam_password"

        // linked setting 2
        private const val SETTING_DOOR_MODULE = "door_module"
        private const val SETTING_CLOSE_DOOR_TIMEOUT = "close_door_timeout"

        // update cycle setting
        private const val SETTING_UPDATE_USER = "update_user"
        private const val SETTING_UPDATE_RECORD = "update_record"
        private const val SETTING_UPDATE_OTHER = "update_other"
        private const val SETTING_CLEAR_DATA = "clear_data"

        // bluetooth setting
        private const val SETTING_BLUETOOTH_PASSWORD = "bluetooth_password"
        private const val SETTING_BLUETOOTH_ADDRESS = "bluetooth_address"
        private const val SETTING_BLUETOOTH_DEVICE_NAME = "bluetooth_device_name"
    }

    override var isObbLoaded: Boolean = false
    override var isDeviceDbExist: Boolean = false
    override var clockSerialNumber: Int = 1

    override var languageId: Int? = null

    // connection setting
    override var serverIp: String = ""
    override var webSocketIp: String = ""
    override var ftpAccount: String = ""
    override var ftpPassword: String = ""
    override var tabletName: String = ""
    override var tabletToken: String = ""
    override var isLoginFinish: Boolean = false

    // function setting
    override var idleTime: Long = 0
    override var holdResultTime: Long = 0
    override var faceVerifyTime: Long = 0
    override var applicationMode: Int = 0
    override var checkMode: Int = 0
    override var isEnableHumanDetection: Boolean = false
    override var isFdrOnlineMode: Boolean = true
    override var isScreenSaverEnable: Boolean = true

    // linked setting
    override var webcamType: Int = 0
    override lateinit var webcamUrl: String
    override lateinit var webcamUsername: String
    override lateinit var webcamPassword: String

    // linked setting 2
    override var doorModule: Int = 0
    override var closeDoorTimeout: Long = 0

    // update cycle setting
    override var updateUserTime: Int = 60
    override var updateRecordTime: Int = 10
    override var updateOtherTime: Int = 60
    override var clearDataTime: Int = 5

    // bluetooth setting
    override lateinit var bluetoothPassword: String
    override lateinit var bluetoothAddress: String
    override lateinit var bluetoothDeviceName: String

    override var fetcherListener: FetcherListener? = null

    private fun packPreferencesToJson(): JSONObject {
        //val gson = Gson()

        val jSetting = JSONObject()

        jSetting.put(PREF_OBB_LOADED, isObbLoaded)
        jSetting.put(PREF_DEVICE_DB_EXIST, isDeviceDbExist)
        jSetting.put(PREF_CLOCK_SERIAL_NUM, clockSerialNumber)

        languageId?.let {
            jSetting.put(PREF_LANGUAGE_ID, it)
        }

        // connection setting
        jSetting.put(SETTING_SERVER_IP, serverIp)
        jSetting.put(SETTING_WEB_SOCKET_IP, webSocketIp)
        jSetting.put(SETTING_FTP_ACCOUNT, ftpAccount)
        jSetting.put(SETTING_FTP_PASSWORD, ftpPassword)
        jSetting.put(SETTING_TABLET_NAME, tabletName)
        jSetting.put(SETTING_TABLET_TOKEN, tabletToken)
        jSetting.put(SETTING_IS_LOGIN_FINISH, isLoginFinish)

        // function setting
        jSetting.put(SETTING_IDLE_TIME, idleTime)
        jSetting.put(SETTING_HOLD_RESULT_TIME, holdResultTime)
        jSetting.put(SETTING_FACE_VERIFY_TIME, faceVerifyTime)
        jSetting.put(SETTING_APPLICATION_MODE, applicationMode)
        jSetting.put(SETTING_CHECK_MODE, checkMode)
        jSetting.put(SETTING_ENABLE_HUMAN_DETECTION, isEnableHumanDetection)
        jSetting.put(SETTING_ENABLE_FDR_ONLINE_MODE, isFdrOnlineMode)
        jSetting.put(SETTING_ENABLE_SCREEN_SAVER, isScreenSaverEnable)

        // linked setting
        jSetting.put(SETTING_WEB_CAM, webcamType)
        jSetting.put(SETTING_WEB_CAM_URL, webcamUrl)
        jSetting.put(SETTING_WEB_CAM_USERNAME, webcamUsername)
        jSetting.put(SETTING_WEB_CAM_PASSOWRD, webcamPassword)
        jSetting.put(SETTING_DOOR_MODULE, doorModule)

        // update cycle setting
        jSetting.put(SETTING_UPDATE_USER, updateUserTime)
        jSetting.put(SETTING_UPDATE_RECORD, updateRecordTime)
        jSetting.put(SETTING_UPDATE_OTHER, updateOtherTime)
        jSetting.put(SETTING_CLEAR_DATA, clearDataTime)

        // bluetooth setting
        jSetting.put(SETTING_CLOSE_DOOR_TIMEOUT, closeDoorTimeout)
        jSetting.put(SETTING_BLUETOOTH_PASSWORD, bluetoothPassword)
        jSetting.put(SETTING_BLUETOOTH_ADDRESS, bluetoothAddress)
        jSetting.put(SETTING_BLUETOOTH_DEVICE_NAME, bluetoothDeviceName)

        return jSetting
    }

    private fun initPreferences() {
        // connection setting
        //serverIp = Constants.DEFAULT_SERVER_IP
        //ftpIp = Constants.DEFAULT_FTP_IP
        //webSocketIp = Constants.DEFAULT_WEB_SOCKET_IP
        //ftpAccount = Constants.DEFAULT_FTP_ACCOUNT
        //ftpPassword = Constants.DEFAULT_FTP_PASSWORD
        //tabletName = Constants.DEFAULT_TABLET_NAME
        //tabletToken = Constants.DEFAULT_TABLET_TOKEN

        // function setting
        idleTime = Constants.DEFAULT_IDEL_TIME
        holdResultTime = Constants.DEFAULT_HOLD_RESULT_TIME
        faceVerifyTime = Constants.DEFAULT_FACE_VERIFY_TIME
        applicationMode = Constants.DEFAULT_APPLICATION_MODE
        checkMode = Constants.DEFAULT_CHECK_MODE
        isEnableHumanDetection = Constants.DEFAULT_ENABLE_HUMAN_DETECTION
        isFdrOnlineMode = Constants.DEFAULT_FDR_ONLINE_MODE
        isScreenSaverEnable = Constants.DEFAULT_SCREEN_SAVER_ENABLE

        // linked setting
        webcamType = Constants.DEFAULT_WEB_CAM
        webcamUrl = ""
        webcamUsername = ""
        webcamPassword = ""
        doorModule = Constants.DEFAULT_DOOR_MODULE

        // update cycle setting
        updateUserTime = Constants.DEFAULT_UPDATE_USER_TIME
        updateRecordTime = Constants.DEFAULT_UPDATE_RECORD_TIME
        updateOtherTime = Constants.DEFAULT_UPDATE_OTHER_TIME
        clearDataTime = Constants.DEFAULT_CLEAR_DATA_TIME

        // bluetooth setting
        closeDoorTimeout = Constants.DEFAULT_CLOSE_DOOR_TIMEOUT
        bluetoothPassword = Constants.DEFAULT_BLUETOOTH_PASSWORD
        bluetoothAddress = ""
        bluetoothDeviceName = "Unknown"

        applyNewSetting()
    }

    override fun readPreferences() {
        val preferences = context.getSharedPreferences("preference", Context.MODE_PRIVATE)

        // to check whether use the setting in the same version
        val versionName = preferences.getString(APP_VERSION, "")
        if (versionName != getVersionName()) {
            Timber.log(Log.DEBUG, "Read reference failed, previous version ${getVersionName()}")
            initPreferences()
            return
        }

        val jSettings = JSONObject(preferences.getString(APP_SETTING, ""))
        try {
            languageId = jSettings.getInt(PREF_LANGUAGE_ID)
        } catch (e: Exception) {
            Timber.d("Never set language id before")
        }

        try {
            Timber.log(Log.DEBUG, "Read preference Setting: $jSettings")

//            val gson = Gson()
//            recentKeywordList = gson.fromJson(jSettings.getString(SETTING_RECENT_KEYWORD)
//                                        , object: TypeToken<ArrayList<RecentKeywordItem>>(){}.type)

            isObbLoaded = jSettings.getBoolean(PREF_OBB_LOADED)
            isDeviceDbExist = jSettings.getBoolean(PREF_DEVICE_DB_EXIST)
            clockSerialNumber = jSettings.getInt(PREF_CLOCK_SERIAL_NUM)

            // connection setting
            serverIp = jSettings.getString(SETTING_SERVER_IP)
            webSocketIp = jSettings.getString(SETTING_WEB_SOCKET_IP)
            ftpAccount = jSettings.getString(SETTING_FTP_ACCOUNT)
            ftpPassword = jSettings.getString(SETTING_FTP_PASSWORD)
            tabletName = jSettings.getString(SETTING_TABLET_NAME)
            tabletToken = jSettings.getString(SETTING_TABLET_TOKEN)
            isLoginFinish = jSettings.getBoolean(SETTING_IS_LOGIN_FINISH)

            // function setting
            idleTime = jSettings.getLong(SETTING_IDLE_TIME)
            holdResultTime = jSettings.getLong(SETTING_HOLD_RESULT_TIME)
            faceVerifyTime = jSettings.getLong(SETTING_FACE_VERIFY_TIME)
            applicationMode = jSettings.getInt(SETTING_APPLICATION_MODE)
            checkMode = jSettings.getInt(SETTING_CHECK_MODE)
            isEnableHumanDetection = jSettings.getBoolean(SETTING_ENABLE_HUMAN_DETECTION)
            isFdrOnlineMode = jSettings.getBoolean(SETTING_ENABLE_FDR_ONLINE_MODE)
            isScreenSaverEnable = jSettings.getBoolean(SETTING_ENABLE_SCREEN_SAVER)

            // linked setting
            webcamType = jSettings.getInt(SETTING_WEB_CAM)
            webcamUrl = jSettings.getString(SETTING_WEB_CAM_URL)
            webcamUsername = jSettings.getString(SETTING_WEB_CAM_USERNAME)
            webcamPassword = jSettings.getString(SETTING_WEB_CAM_PASSOWRD)
            doorModule = jSettings.getInt(SETTING_DOOR_MODULE)

            // update cycle setting
            updateUserTime = jSettings.getInt(SETTING_UPDATE_USER)
            updateRecordTime = jSettings.getInt(SETTING_UPDATE_RECORD)
            updateOtherTime = jSettings.getInt(SETTING_UPDATE_OTHER)
            clearDataTime = jSettings.getInt(SETTING_CLEAR_DATA)

            // bluetooth setting
            closeDoorTimeout = jSettings.getLong(SETTING_CLOSE_DOOR_TIMEOUT)
            bluetoothPassword = jSettings.getString(SETTING_BLUETOOTH_PASSWORD)
            bluetoothAddress = jSettings.getString(SETTING_BLUETOOTH_ADDRESS)
            bluetoothDeviceName = jSettings.getString(SETTING_BLUETOOTH_DEVICE_NAME)
        } catch (e: Exception) {
            initPreferences()
            Timber.log(Log.DEBUG, e.toString())
        }

        applyNewSetting()
    }

    override fun savePreferences() {
        Timber.log(Log.DEBUG, "savePreferences()")
        val preferences = context.getSharedPreferences("preference", Context.MODE_PRIVATE)
        val preferenceEditor = preferences.edit()

        preferenceEditor.putString(APP_SETTING, packPreferencesToJson().toString())
        preferenceEditor.putString(APP_VERSION, getVersionName())
        preferenceEditor.apply()
    }

    override fun applyNewSetting() {
        // sync setting
        DeviceUtils.applySetting(this)
    }

    /**`
     * Get App version name
     */
    private fun getVersionName(): String {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return packageInfo.versionName
    }
}