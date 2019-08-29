package com.gorilla.attendance.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.util.Log
import com.crashlytics.android.Crashlytics
import timber.log.Timber
import java.io.File
import java.io.File.separator
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/8/8
 * Description:
 */
class FileLoggingTree(private val context: Context) : Timber.DebugTree() {

    companion object {
        private const val CRASHLYTICS_KEY_PRIORITY = "priority"
        private const val CRASHLYTICS_KEY_TAG = "tag"
        private const val CRASHLYTICS_KEY_MESSAGE = "message"
    }

    @SuppressLint("LogNotTimber")
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {
            val direct = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() +  "/AttendanceLogs")

            if (!direct.exists()) {
                direct.mkdir()
            }

            val fileNameTimeStamp = SimpleDateFormat("dd-MM-yyyy_hh", Locale.getDefault()).format(Date())
            val logTimeStamp = SimpleDateFormat("E MMM dd yyyy 'at' hh:mm:ss:SSS aaa", Locale.getDefault()).format(Date())

            val fileName = "$fileNameTimeStamp.html"

            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/AttendanceLogs" + separator + fileName)

            file.createNewFile()

            if (file.exists()) {

                val fileOutputStream = FileOutputStream(file, true)

                fileOutputStream.write("<p style=\"background:lightgray;\"><strong style=\"background:lightblue;\">&nbsp&nbsp$logTimeStamp :&nbsp&nbsp</strong>&nbsp&nbsp$tag&nbsp&nbsp$message</p>".toByteArray())
                fileOutputStream.close()
            }
        } catch (e: Exception) {
            Log.e("FileLoggingTree", "Error while logging into file : $e")
        }

        if (priority == Log.ERROR) {
            Crashlytics.setInt(CRASHLYTICS_KEY_PRIORITY, priority)
            Crashlytics.setString(CRASHLYTICS_KEY_TAG, tag)
            Crashlytics.setString(CRASHLYTICS_KEY_MESSAGE, message)

            if (t == null) {
                Crashlytics.logException(Exception(message))
            } else {
                Crashlytics.logException(t)
            }
        }
    }
}