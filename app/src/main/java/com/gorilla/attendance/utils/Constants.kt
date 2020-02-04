package com.gorilla.attendance.utils

import com.gorilla.attendance.data.model.SettingAccount


/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/6/19
 * Description:
 */
object Constants {
    const val LOCALE_TW = "zh_TW"
    const val LOCALE_EN = "en_US"
    const val LOCALE_CN = "zh_CN"

    const val PERMISSION_DENY_CLOSE_TIME = 3000L

    const val UPDATE_USER_SUCCESS_MAGIC = "000001"

    /**
     * Default values
     */
    const val REGISTER_MODE = 0
    const val VERIFICATION_MODE = 1

    const val CHECK_IN = 0
    const val CHECK_OUT = 1
    const val CHECK_OPTION = 2

    const val ANDROID_BUILD_IN_LENS = 0
    const val RTSP_WEB_CAM = 1

    const val BLUETOOTH = 0
    const val COM_USB_RELAY = 1

    const val CLOCK_UNKNOWN = "UNKNOWN"
    const val LIVENESS_OFF = "OFF"
    const val LIVENESS_SUCCEED = "SUCCEED"
    const val LIVENESS_FAILED = "FAILED"

    const val FACE_VERIFY_SUCCEED = "SUCCEED"
    const val FACE_VERIFY_FAILED = "FAILED"
    const val FACE_VERIFY_IDENTIFIED = "IDENTIFIED"
    const val FACE_VERIFY_NONE = "NONE"

    const val RECORD_MODE_RECORD = "RECORD"
    const val RECORD_MODE_UNRECOGNIZED = "UNRECOGNIZED"

    const val USER_TYPE_EMPLOYEE = "EMPLOYEE"
    const val USER_TYPE_VISITOR = "VISITOR"
    const val IMAGE_FORMAT_PNG = "png"

    /**
     * About face recognition
     */
    const val FDR_VERIFY_SUCCESS = 0
    const val FDR_VERIFY_FAILED  = 1

    const val USER_REGISTER_SUCCESS = 2
    const val USER_REGISTER_FAILED = 3

    const val UI_FACE_UNKNOWN = 0
    const val UI_FACE_VALID = 1
    const val UI_REGISTER_COMPLETE = 2
    const val UI_REGISTER_FAILED = 3
    //////////////////////////////////////////////////////////////////////////////

    /**
     * about registration
     */
    const val REGISTER_STATE_SCAN_CODE = 0
    const val REGISTER_STATE_FILL_FORM = 1
    const val REGISTER_STATE_FACE_GET = 2
    const val REGISTER_STATE_COMPLETE = 3

    // setting username, password
    val settingAccounts = arrayOf(
        SettingAccount("aa", "aa"),
        SettingAccount("system01", "admin01"),
        SettingAccount("system02", "admin02")
    )

    // connection setting
    const val DEFAULT_SERVER_IP = "192.168.11.158"
    const val DEFAULT_FTP_IP = "192.168.11.158"
    const val DEFAULT_WEB_SOCKET_IP = "192.168.11.158"
    const val DEFAULT_FTP_ACCOUNT = "gorilla"
    const val DEFAULT_FTP_PASSWORD = "gorillakm"
    const val DEFAULT_TABLET_NAME = "ASUS_TC"
    const val DEFAULT_TABLET_TOKEN = "0604bebd94c4-403a9a73-77ac-5998-1da1"

    // function setting
    const val DEFAULT_IDEL_TIME = 30L
    const val DEFAULT_HOLD_RESULT_TIME = 1300L
    const val DEFAULT_FACE_VERIFY_TIME = 10000L
    const val DEFAULT_APPLICATION_MODE = VERIFICATION_MODE
    const val DEFAULT_CHECK_MODE = CHECK_IN
    const val DEFAULT_ENABLE_HUMAN_DETECTION = false
    const val DEFAULT_FDR_ONLINE_MODE = false
    const val DEFAULT_SCREEN_SAVER_ENABLE = true

    // linked setting
    const val DEFAULT_WEB_CAM = ANDROID_BUILD_IN_LENS
    const val DEFAULT_DOOR_MODULE = BLUETOOTH

    // update cycle setting
    const val DEFAULT_UPDATE_USER_TIME = 60     // secs
    const val DEFAULT_UPDATE_RECORD_TIME = 10   // secs
    const val DEFAULT_UPDATE_OTHER_TIME = 60    // secs
    const val DEFAULT_CLEAR_DATA_TIME = 5   // min

    // bluetooth setting
    const val DEFAULT_CLOSE_DOOR_TIMEOUT = 5L
    const val DEFAULT_BLUETOOTH_PASSWORD = "12345678"
}