package com.gorilla.attendance.utils

import com.gorilla.attendance.data.model.Videos
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import javax.inject.Inject

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/7/19
 * Description:
 */
class VideoManager @Inject constructor(private val mPreferences: AppPreferences) {

    private val disposables = ArrayList<Disposable>()

    private fun deleteVideoFiles() {
        Timber.d("deleteVideoFiles()")

        val appContentDir = File(DeviceUtils.SD_CARD_APP_CONTENT)
        val files = appContentDir.listFiles()

        for (file in files) {
            Timber.d("Delete ${file.absoluteFile}, status: ${file.delete()}")
        }
    }

    private fun isAlreadyHaveFile(video: Videos): Boolean {
        val appContentDir = File(DeviceUtils.SD_CARD_APP_CONTENT)
        if (!appContentDir.exists()) {
            return false
        }

        val files = appContentDir.listFiles { file ->
            file.isFile
        }

        if (files.isEmpty()) {
            return false
        }

        for (file in files) {
            if (file.name == video.name) {
                // file size not match, need to download again
                Timber.d("file length = ${file.length()}")
                Timber.d("video file size = ${video.fileSize}")
                if (file.length() != video.fileSize) {
                    Timber.d("File size not match, file: ${file.name}")
                    file.delete()
                    return false
                }

                return true
            }
        }

        return false
    }

    fun downloadEmptyVideo() {
        Timber.d("downloadEmptyVideo()")
        Timber.d("downloadVideoFromFtp(), FTP ip address = ${mPreferences.ftpIp}")
        Timber.d("downloadVideoFromFtp(), FTP account = ${mPreferences.ftpAccount}")
        Timber.d("downloadVideoFromFtp(), FTP password = ${mPreferences.ftpPassword}")

        DeviceUtils.deviceVideos?.let {
            for (video in it) {
                if (!isAlreadyHaveFile(video)) {
                    // Start download video
                    downloadVideoFromFtp(video.name, video.url)
                }
            }
        }
    }

    fun updateAllVideos() {
        Timber.d("updateAllVideos(), invoke because web socket synchronization")
        Timber.d("downloadVideoFromFtp(), FTP ip address = ${mPreferences.ftpIp}")
        Timber.d("downloadVideoFromFtp(), FTP account = ${mPreferences.ftpAccount}")
        Timber.d("downloadVideoFromFtp(), FTP password = ${mPreferences.ftpPassword}")

        stopDownload()

        // Delete all files in the path
        deleteVideoFiles()

        DeviceUtils.deviceVideos?.let {
            // Start download all videos
            for (video in it) {
                downloadVideoFromFtp(video.name, video.url)
            }
        }
    }

    private fun downloadVideoFromFtp(fileName: String?, ftpPath: String?) {
        val disposable = Observable.just(Optional("downloadVideoFromFtp()"))
            .subscribeOn(Schedulers.newThread())
            .subscribe {
                Timber.d("downloadFromFtp(), file name = $fileName")
                Timber.d("downloadVideoFromFtp(), FTP url path = $ftpPath")

                // e.g., ftp://gorilla:gorillakm@192.168.11.158/Video/f3/9d/0c57ab7c3245-4e65ada6-8114-8aff-f39d.mp4
                val videoPath = ftpPath?.substring(ftpPath.indexOf("Video/"))

                val downloadFtpUrl = String.format(
                    "ftp://%s:%s@%s/%s",
                    mPreferences.ftpAccount, mPreferences.ftpPassword, mPreferences.ftpIp, videoPath
                )
                Timber.d("downloadFtpUrl = $downloadFtpUrl")

                try {
                    val url = URL(downloadFtpUrl)
                    val conn = url.openConnection()
                    val inputStream = conn.getInputStream()

                    val outputStream = FileOutputStream(DeviceUtils.SD_CARD_APP_CONTENT + "/" + fileName)

                    var read: Int
                    Timber.d("downloading FTP Video, file name: $fileName")
                    inputStream.use { input ->
                        outputStream.use { ouput ->
                            while (input.read().also { read = it } != -1) {
                                ouput.write(read)
                            }
                        }
                    }
                    Timber.d("download FTP Video finished, file name: $fileName")
                } catch (e: IOException) {
                    Timber.e(e.message)
                }
            }

        disposables.add(disposable)
    }

    private fun stopDownload() {
        for (disposable in disposables) {
            if (!disposable.isDisposed) {
                disposable.dispose()
            }
        }
        disposables.clear()
    }
}