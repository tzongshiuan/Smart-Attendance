package com.gorilla.attendance.utils

import com.gorilla.attendance.data.model.Videos
import com.gorilla.attendance.ui.common.SharedViewModel
import com.gorilla.attendance.ui.screenSaver.ScreenSaverViewModel
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

    private var mSharedViewModel: SharedViewModel? = null
    private var mScreenSaverViewModel: ScreenSaverViewModel? = null

    private val disposables = ArrayList<Disposable>()

    var isDownloading = false

    private fun deleteVideoFiles() {
        Timber.d("deleteVideoFiles()")

        val appContentDir = File(DeviceUtils.SD_CARD_APP_CONTENT)
        val files = appContentDir.listFiles()

        for (file in files) {
            Timber.d("Delete ${file.absoluteFile}, status: ${file.delete()}")
        }
    }

    private fun deleteVideoFile(fileName: String?) {
        Timber.d("deleteVideoFiles()")

        val appContentDir = File(DeviceUtils.SD_CARD_APP_CONTENT)
        val files = appContentDir.listFiles()

        if (files != null && files.isNotEmpty()) {
            for (file in files) {
                if (file.name == fileName) {
                    Timber.d("Delete ${file.absoluteFile}, status: ${file.delete()}")
                    return
                }
            }
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
//                Timber.d("file length = ${file.length()}")
//                Timber.d("video file size = ${video.fileSize}")
//                if (file.length() != video.fileSize) {
//                    Timber.d("File size not match, file: ${file.name}")
//                    file.delete()
//                    return false
//                }

                return true
            }
        }

        return false
    }

    private fun setDownloadStatus() {
        val appContentDir = File(DeviceUtils.SD_CARD_APP_CONTENT)
        if (!appContentDir.exists()) {
            return
        }

        val files = appContentDir.listFiles { file ->
            file.isFile
        }

        if (files.isEmpty()) {
            return
        }

        var isFind: Boolean
        DeviceUtils.deviceVideos?.let { videos ->
            for (video in videos) {
                isFind = false

                for (file in files) {
                    if (file.name == video.name) {
                        isFind = true
                        break
                    }
                }

                if (!isFind) {
                    return
                }
            }
        }

        isDownloading = false
    }

    fun downloadEmptyVideo(sharedViewModel: SharedViewModel, screenSaverViewModel: ScreenSaverViewModel) {
        if (mSharedViewModel == null) {
            mSharedViewModel = sharedViewModel
        }

        if (mScreenSaverViewModel == null) {
            mScreenSaverViewModel = screenSaverViewModel
        }

        Timber.i("downloadEmptyVideo()")
        Timber.i("downloadVideoFromFtp(), FTP ip address = ${mPreferences.serverIp}")
        Timber.i("downloadVideoFromFtp(), FTP account = ${mPreferences.ftpAccount}")
        Timber.i("downloadVideoFromFtp(), FTP password = ${mPreferences.ftpPassword}")

        synchronized(isDownloading) {
            if (!isDownloading) {
                DeviceUtils.deviceVideos?.let {
                    for (video in it) {
                        if (!isAlreadyHaveFile(video)) {
                            // Start download video
                            isDownloading = true
                            downloadVideoFromFtp(video)
                        }
                    }
                }
            }
        }
    }

    fun updateAllVideos(sharedViewModel: SharedViewModel, screenSaverViewModel: ScreenSaverViewModel) {
        if (mSharedViewModel == null) {
            mSharedViewModel = sharedViewModel
        }

        if (mScreenSaverViewModel == null) {
            mScreenSaverViewModel = screenSaverViewModel
        }

        Timber.d("updateAllVideos(), [WEB SOCKET], invoke because web socket synchronization")
        Timber.d("downloadVideoFromFtp(), FTP ip address = ${mPreferences.serverIp}")
        Timber.d("downloadVideoFromFtp(), FTP account = ${mPreferences.ftpAccount}")
        Timber.d("downloadVideoFromFtp(), FTP password = ${mPreferences.ftpPassword}")

        synchronized(isDownloading) {
            stopDownload()

            //if (!isDownloading) {
                // Delete all files in the path
                isDownloading = true

                deleteVideoFiles()

                DeviceUtils.deviceVideos?.let {
                    // Start download all videos
                    for (video in it) {
                        downloadVideoFromFtp(video)
                    }
                }

                if (DeviceUtils.deviceVideos == null || DeviceUtils.deviceVideos?.size == 0) {
                    isDownloading = false
                }
            //}
        }
    }

    @Synchronized
    private fun downloadVideoFromFtp(video: Videos) {
        val disposable = Observable.just(Optional("downloadVideoFromFtp()"))
            .subscribeOn(Schedulers.io())
            .subscribe {
                Timber.i("downloadFromFtp(), file name = ${video.name}")
                Timber.i("downloadVideoFromFtp(), FTP url path = ${video.url}")

                // e.g., ftp://gorilla:gorillakm@192.168.11.158/Video/f3/9d/0c57ab7c3245-4e65ada6-8114-8aff-f39d.mp4
                val url = video.url
                val videoPath = url?.substring(url.indexOf("Video/"))

                val downloadFtpUrl = String.format(
                    "ftp://%s:%s@%s/%s",
                    mPreferences.ftpAccount, mPreferences.ftpPassword, mPreferences.serverIp, videoPath
                )
                Timber.d("downloadFtpUrl = $downloadFtpUrl")

                try {
                    Timber.d("downloading FTP Video, file name: ${video.name}")

                    val conn = URL(downloadFtpUrl).openConnection()
                    val inputStream = conn.getInputStream()
                    val outputStream = FileOutputStream(DeviceUtils.SD_CARD_APP_CONTENT + "/" + video.name)

                    val buffer = ByteArray(BUFFER_SIZE)
                    var read: Int

                    inputStream.use { fis ->
                        outputStream.use { fos ->
                            while (fis.read(buffer).also { read = it } != -1) {
                                fos.write(buffer, 0, read)
                            }
                        }
                    }
                    mSharedViewModel?.toastEvent?.postValue("Download video finished, file name: ${video.name}")
                    Timber.d("Download video finished, file name: ${video.name}")
                    setDownloadStatus()

                    mScreenSaverViewModel?.syncVideosEvent?.postValue(true)
                } catch (e: IOException) {
                    Timber.e(e)
                    deleteVideoFile(video.name)
                    downloadVideoFromFtp(video)
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

    companion object {
        private const val BUFFER_SIZE = 4096 * 16
    }
}