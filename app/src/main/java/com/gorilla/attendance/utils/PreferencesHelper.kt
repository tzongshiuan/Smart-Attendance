package com.gorilla.attendance.utils

import com.gorilla.attendance.ui.common.FetcherListener


interface PreferencesHelper {
    var isObbLoaded: Boolean
    var isDeviceDbExist: Boolean
    var clockSerialNumber: Int

    var languageId: Int?

    // connection setting
    var serverIp: String
    var ftpIp: String
    var webSocketIp: String
    var ftpAccount: String
    var ftpPassword: String
    var tabletName: String
    var tabletToken: String

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
    var doorModule: Int

    // bluetooth setting
    var closeDoorTimeout: Long
    var bluetoothPassword: String
    var bluetoothAddress: String
    var bluetoothDeviceName: String

    var fetcherListener: FetcherListener?

    fun readPreferences()
    fun savePreferences()

    fun applyNewSetting()
}