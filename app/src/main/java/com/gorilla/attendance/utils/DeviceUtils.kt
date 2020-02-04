package com.gorilla.attendance.utils

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Environment
import android.util.Log
import android.view.View
import com.gorilla.attendance.R
import com.gorilla.attendance.data.model.Marquees
import com.gorilla.attendance.data.model.Videos
import com.jakewharton.rxbinding.view.RxView
import kotlinx.android.synthetic.main.register_hint_dialog.view.*
import timber.log.Timber
import java.io.File
import java.net.URI
import java.util.*

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/6/19
 * Description:
 */
object DeviceUtils {
    const val TIMER_DELAYED_TIME = 1000L  // 1 second

    const val ENTER_SETTING_CLICK_NUM = 5   // 5 times
    const val ENTER_SETTING_INTERVAL_TIME = 3000L  // 3 seconds

    const val CHOOSE_CLOCK_TYPE_TIME = 6000L  // 6 seconds

    const val EXIST_PROFILE_SKIP_TIME = 2000L // 2 seconds

    // about screen saver
    const val BACK_TO_HOME_DELAYED_TIME  = 30000L  // 30 seconds

    const val SAFE_FDR_INTERVAL_TIME = 1300L  // 1.3 seconds
    var stopFdrOnDestroyTime: Long = 0L

    const val PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAp90ojTJOc6o/QDkhixywIjy7e032JpzSY+5TdZ1Cp+QHlzFs8t5xboCvJQClpr/kRy++p80gYfCyC2A3deQwS5hO2i4NvR1M2UShCCr8tfTtFcSDhcvXYJrOWvO0hB/y2Oe8oz3W5E5V1HRuo1DfJxfA/9sBIhOdR9iF/lr4jiwyNxJsUa+MfO5411UPn5WWJhdtXMv4qaBA4iwLyj1dZXiiHjG2+nFHgBABWIcLvYvLrj1kCG7+c02dXkayD/t9FB1O8gy5M57kRqvneExgMBn6wuAomEi5u/T47DEEog1jyGCglLcLZrh4wbFtRSthStZXOYPpbPjWrFB6TFwpHQIDAQAB"
    const val FDR_SO_FILE_LENGTH = 223159052L
    val xApks = arrayOf(XAPKFile(true, 1, 189759330L))

    var mLocale: String? = null

    private const val FDR_SO_FILE_NAME = "libisscore_fdr.so"
    var APP_FOLDER: String? = null
    var APP_OBB_FOLDER: String? = null
    var APP_INTERNAL_BIN_FOLDER: String? = null
    var APP_PHOTO_FOLDER: String? = null


    // bluetooth
    @Volatile var isBluetoothDisconnected = true
    const val BLUETOOTH_CHECK_INTERVAL_TIME = 5000L


    val SD_CARD_APP = Environment.getExternalStorageDirectory().absolutePath + "/SmartAttendance"
    val SD_CARD_APP_CONTENT = "$SD_CARD_APP/content"
    val SD_CARD_APP_APK = "$SD_CARD_APP/Apk"
    val SD_CARD_APP_LOG = "$SD_CARD_APP/log"
    val SD_CARD_APP_FACE_IMAGE = "$SD_CARD_APP/FACE_Images"


    val mFacePngList: ArrayList<ByteArray> = ArrayList()
    val VERIFIED_CANDIDATE_NUMBER = 1
    var mFdrRange = 80


    // about web socket
    const val WEB_SOCKET_PATH = "/SmartEnterprise/V1_1beta/WebSocketApi"
    lateinit var mWsUri: URI
    const val WEB_SOCKET_TIME_OUT = 10000L    // 10 seconds
    const val CHECK_WEB_SOCKET_CLOSE_ERROR_TIME = 5000L    // 5 seconds
    const val CHECK_WEB_SOCKET_TIME = 90000L  // 90 seconds


    // about screen saver
    var deviceMarquees: ArrayList<Marquees>? = null
    var deviceVideos: ArrayList<Videos>? = null


    /**
     * Useless now, control the language is controlled by client
     */
    fun setDeviceInfo(context: Context, locale: String) {
        Timber.d("Current locale: $locale")
        mLocale = locale

//        val resources = context.resources
//        //val dm = resources.displayMetrics
//        val config = resources.configuration
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            config.setLocale(Locale.ENGLISH)
//            when (mLocale) {
//                Constants.LOCALE_EN -> config.setLocale(Locale.ENGLISH)
//                Constants.LOCALE_TW -> config.setLocale(Locale.TRADITIONAL_CHINESE)
//                Constants.LOCALE_CN -> config.setLocale(Locale.SIMPLIFIED_CHINESE)
//                else -> config.setLocale(Locale.ENGLISH)
//            }
//        } else {
//            when (mLocale) {
//                Constants.LOCALE_EN -> config.locale = Locale.ENGLISH
//                Constants.LOCALE_TW -> config.locale = Locale.TRADITIONAL_CHINESE
//                Constants.LOCALE_CN -> config.locale = Locale.SIMPLIFIED_CHINESE
//                else -> config.locale = Locale.ENGLISH
//            }
//        }
//
//        context.createConfigurationContext(config)
    }

    fun getValidRtspUrl(url: String): String {
        return if (url.startsWith("rtsp")) {
            url
        } else {
            "rtsp://$url"
        }
    }

    fun initFolder(context: Context) {
        APP_FOLDER = Environment.getExternalStorageDirectory().toString() + "/Android/data/" + context.packageName
        APP_OBB_FOLDER = Environment.getExternalStorageDirectory().toString() + "/Android/obb/" + context.packageName
        APP_INTERNAL_BIN_FOLDER = context.filesDir.toString() + "/Bin"
        APP_PHOTO_FOLDER = "$APP_FOLDER/photo"
        createFolder(APP_FOLDER)
        createFolder(APP_OBB_FOLDER)
        createFolder(APP_INTERNAL_BIN_FOLDER)
        createFolder(APP_PHOTO_FOLDER)
        createFolder(SD_CARD_APP_CONTENT)
        Timber.log(Log.DEBUG, "APP_FOLDER, path: $APP_FOLDER")
        Timber.log(Log.DEBUG, "APP_OBB_FOLDER, path: $APP_OBB_FOLDER")
        Timber.log(Log.DEBUG, "APP_INTERNAL_BIN_FOLDER, path: $APP_INTERNAL_BIN_FOLDER")
        Timber.log(Log.DEBUG, "APP_PHOTO_FOLDER, path: $APP_PHOTO_FOLDER")
    }

    private fun createFolder(path: String?) {
        val file = File(path)
        if (!file.exists()) {
            file.mkdirs()
        }
    }

    fun getInternalSOFilePath(): String {
        return APP_INTERNAL_BIN_FOLDER + File.separator + FDR_SO_FILE_NAME
    }

    fun applySetting(preferences: AppPreferences) {
        mWsUri = URI("ws://${preferences.webSocketIp}$WEB_SOCKET_PATH")
    }

    fun showRegisterHintDialog(context: Context?, message: String) {
        if (context == null) {
            Timber.e("context == null")
            return
        }

        val builder = AlertDialog.Builder(context)
        builder.setCancelable(true)

        val view = View.inflate(context, R.layout.register_hint_dialog, null)
        builder.setView(view)

        view.dialogTitleText.text = context.getString(R.string.form_dialog_title)
        view.messageText.text = message

        val dialog = builder.create().also {
            it.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.show()
        }

        RxView.clicks(view)
            .subscribe {
                dialog.dismiss()
            }
    }
}