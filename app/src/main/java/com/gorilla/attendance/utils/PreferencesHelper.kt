package com.gorilla.attendance.utils

import com.gorilla.attendance.ui.common.FetcherListener


interface PreferencesHelper {
    var isObbLoaded: Boolean
    var isDeviceDbExist: Boolean
    var clockSerialNumber: Int

    var languageId: Int?

    // connection setting
    var serverIp: String
    var webSocketIp: String
    var ftpAccount: String
    var ftpPassword: String
    var tabletName: String
    var tabletToken: String
    var isLoginFinish: Boolean

    // function setting
    var idleTime: Long
    var holdResultTime: Long
    var faceVerifyTime: Long
    var applicationMode: Int
    var checkMode: Int
    var isEnableHumanDetection: Boolean
    var isFdrOnlineMode: Boolean
    var isScreenSaverEnable: Boolean

    // linked setting
    var webcamType: Int
    var webcamUrl: String
    var webcamUsername: String
    var webcamPassword: String

    // linked setting 2
    var doorModule: Int
    var closeDoorTimeout: Long

    // update cycle setting
    var updateUserTime: Int
    var updateRecordTime: Int
    var updateOtherTime: Int
    var clearDataTime: Int

    // bluetooth setting
    var bluetoothPassword: String
    var bluetoothAddress: String
    var bluetoothDeviceName: String

    var fetcherListener: FetcherListener?

    fun readPreferences()
    fun savePreferences()

    fun applyNewSetting()
}