package com.gorilla.attendance.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Messenger
import android.os.SystemClock
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.android.vending.expansion.zipfile.ZipResourceFile
import com.google.android.vending.expansion.downloader.*
import com.google.android.vending.expansion.downloader.impl.DownloaderService
import com.gorilla.attendance.service.ObbDownloaderService
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.*
import java.util.zip.CRC32
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/6/26
 * Description:
 */
@Singleton
class ObbManager @Inject constructor(compositeDisposable: CompositeDisposable) : IDownloaderClient {
    companion object {
        const val OBB_STATE_DOWNLOAD_XAPKS = 2001
        const val OBB_STATE_CHECK_DOWNLOAD_XAPKS = 2002
        const val OBB_STATE_UNZIP_OBB = 2003
        const val OBB_STATE_CHECK_SO = 2004

        private const val SMOOTHING_FACTOR = 0.005f
        private const val PROGRESS_PERIOD = 500L
    }

    private var mCompositeDisposable: CompositeDisposable = compositeDisposable

    // Subscribed by observers, Ex: MainActivity and ObbProgressView
    val downloadStateLiveEvent = SingleLiveEvent<Int>()
    val curProgressLiveEvent = SingleLiveEvent<DownloadProgressInfo>()
    val curStateLiveEvent = SingleLiveEvent<Int>()

    private lateinit var mActivity: AppCompatActivity
    private lateinit var mContext: Context

    private var mXApks = DeviceUtils.xApks
    private var mDownloaderClientStub: IStub? = null
    private var mRemoteService: IDownloaderService? = null
    private var mDownloadState: Int = 0

    var mStateController: ObbStateController = ObbStateController()

    private var isRunning = false

    fun initSetting(activity: AppCompatActivity) {
        mActivity = activity
        mContext = activity

        mStateController.listState.add(OBB_STATE_DOWNLOAD_XAPKS)
        mStateController.listState.add(OBB_STATE_CHECK_DOWNLOAD_XAPKS)
        mStateController.listState.add(OBB_STATE_UNZIP_OBB)
        mStateController.listState.add(OBB_STATE_CHECK_SO)

        initObservers()

        DeviceUtils.initFolder(mActivity)
    }

    private fun initObservers() {
        mStateController.currentStateLiveEvent.observe(mActivity, Observer { state ->
            when (state) {
                OBB_STATE_DOWNLOAD_XAPKS -> downloadXApk()
                OBB_STATE_CHECK_DOWNLOAD_XAPKS -> checkDownloadXApk()
                OBB_STATE_UNZIP_OBB -> unzipObbFile()
                OBB_STATE_CHECK_SO -> checkInternalSoLibrary()
            }
            curStateLiveEvent.postValue(state)
        })
        //mStateController.currentStateLiveEvent.removeObserver {}
    }

    private fun downloadXApk() {
        if (mDownloaderClientStub == null) {
            mDownloaderClientStub = DownloaderClientMarshaller.CreateStub(
                this, ObbDownloaderService::class.java
            )
        }

        try {
            val launchIntent = mActivity.intent
            val intentToLaunchThisActivityFromNotification = Intent(mActivity, mActivity.javaClass)
            intentToLaunchThisActivityFromNotification.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            intentToLaunchThisActivityFromNotification.action = launchIntent.action

            launchIntent.categories?.let {
                for (category in it) {
                    intentToLaunchThisActivityFromNotification.addCategory(category)
                }
            }

            // Build PendingIntent used to open this activity from Notification
            val pendingIntent = PendingIntent.getActivity(
                mActivity, 0, intentToLaunchThisActivityFromNotification, PendingIntent.FLAG_UPDATE_CURRENT
            )

            val startResult = DownloaderClientMarshaller.startDownloadServiceIfRequired(
                mActivity, pendingIntent, ObbDownloaderService::class.java
            )
            if (startResult != DownloaderClientMarshaller.NO_DOWNLOAD_REQUIRED) {
                // The DownloaderService has started downloading the files,
                // show progress
            } // otherwise, download not needed so we fall through to
        } catch (e: Exception) {
            Timber.log(Log.ERROR, "Cannot find the own package!, e: ${e.printStackTrace()}")
        }

        mDownloaderClientStub?.connect(mContext)
    }

    private fun checkDownloadXApk() {
        val disposable = Observable.just(Optional("checkDownloadXApk"))
            .subscribeOn(Schedulers.io())
            .subscribe({
                Timber.d("Function: ${it.value}")

                for (xf in mXApks) {
                    var fileName = Helpers.getExpansionAPKFileName(mContext, xf.mIsMain, xf.mFileVersion)

                    if (!Helpers.doesFileExist(mContext, fileName, xf.mFileSize, false)) {
                        Timber.log(Log.ERROR, "XApk file doesn't exist")
                        mStateController.onStateEnd(false)
                        return@subscribe
                    }

                    // check zip file
                    fileName = Helpers.generateSaveFileName(mContext, fileName)
                    val zrf = ZipResourceFile(fileName)
                    val buf = ByteArray(1024 * 256)
                    try {
                        val entries = zrf.allEntries

                        /**
                         * First calculate the total compressed length
                         */
                        var totalCompressedLength = 0L
                        for (entry in entries) {
                            totalCompressedLength += entry.mUncompressedLength
                        }

                        var averageVerifySpeed = 0F
                        var totalBytesRemaining = totalCompressedLength
                        var timeRemaining: Long

                        /**
                         * Then calculate a CRC for every file in the Zip file,
                         * comparing it to what is stored in the Zip directory.
                         * Note that for compressed Zip files we must extract
                         * the contents to do this comparison.
                         */
                        for (entry in entries) {
                            if (-1L != entry.mCRC32) {
                                var length = entry.mUncompressedLength
                                val crc = CRC32()
                                var dis: DataInputStream? = null
                                try {
                                    dis = DataInputStream(
                                        zrf.getInputStream(entry.mFileName)
                                    )

                                    var startTime = SystemClock.uptimeMillis()
                                    while (length > 0) {
                                        val seek = if (length > buf.size) {
                                            buf.size
                                        } else {
                                            length.toInt()
                                        }
                                        dis.readFully(buf, 0, seek)
                                        crc.update(buf, 0, seek)
                                        length -= seek.toLong()
                                        val currentTime = SystemClock.uptimeMillis()
                                        val timePassed = currentTime - startTime
                                        if (timePassed > 0) {
                                            val currentSpeedSample = seek.toFloat() / timePassed.toFloat()
                                            if (0f != averageVerifySpeed) {
                                                averageVerifySpeed =
                                                    SMOOTHING_FACTOR * currentSpeedSample + (1 - SMOOTHING_FACTOR) * averageVerifySpeed
                                            } else {
                                                averageVerifySpeed = currentSpeedSample
                                            }
                                            totalBytesRemaining -= seek.toLong()
                                            timeRemaining = (totalBytesRemaining / averageVerifySpeed).toLong()

                                            /**
                                             * Publish current progress
                                             */
                                            val progressInfo = DownloadProgressInfo(
                                                totalCompressedLength,
                                                totalCompressedLength - totalBytesRemaining,
                                                timeRemaining,
                                                averageVerifySpeed
                                            )
                                            onDownloadProgress(progressInfo)
                                        }
                                        startTime = currentTime

                                        if (!isRunning) {
                                            mStateController.onStateEnd(false)
                                            return@subscribe
                                        }
                                    }

                                    if (crc.value != entry.mCRC32) {
                                        Timber.log(
                                            Log.ERROR,
                                            "CRC does not match for entry: " + entry.mFileName
                                        )
                                        Timber.log(
                                            Log.ERROR,
                                            "In file: " + entry.zipFileName
                                        )
                                        mStateController.onStateEnd(false)
                                        return@subscribe
                                    }
                                } finally {
                                    dis?.close()
                                }
                            }
                        }
                    } catch (e: IOException) {
                        Timber.log(Log.ERROR, "onError, $e")
                        mStateController.onStateEnd(false)
                        return@subscribe
                    }

                    mStateController.onStateEnd(true)
                }
            }, { throwable -> Timber.log(Log.ERROR, "onError, ${throwable.message}") })

        mCompositeDisposable.add(disposable)
    }

    private fun unzipObbFile() {
        val fileName = Helpers.getExpansionAPKFileName(
            mContext,
            mXApks[0].mIsMain, mXApks[0].mFileVersion
        )
        val zipFilePath = Helpers.generateSaveFileName(mContext, fileName)
        val destFilePath = DeviceUtils.APP_INTERNAL_BIN_FOLDER

        val disposable = Observable.just(Optional("unzipObbFile"))
            .subscribeOn(Schedulers.io())
            .subscribe({
                Timber.d("Function: ${it.value}")

                val buffer = ByteArray(1024)

                try {
                    val zip = ZipFile(zipFilePath)
                    val fis = FileInputStream(zipFilePath)
                    val zis = ZipInputStream(fis)
                    //var zipEntry: ZipEntry? = null
                    var totalCompressedLength = 0L

                    val entries = zip.entries()
                    for (entry in entries) {
                        totalCompressedLength += entry.size
                    }

                    var averageVerifySpeed = 0F
                    var totalBytesRemaining = totalCompressedLength
                    var timeRemaining = 0L

                    var lastTimeSendProgress = SystemClock.uptimeMillis()
                    var mSeek = 0

                    // start to unzip
                    var ze = zis.nextEntry
                    while (ze != null) {
                        Timber.d("Unzipping ${ze.name}")
                        if (ze.isDirectory) {
                            dirChecker(destFilePath, ze.name)
                        } else {
                            // Do the update progress of progress bar here
                            Timber.d("destination path: $destFilePath${File.separator}${ze.name}")

                            val fout = FileOutputStream(destFilePath + File.separator + ze.name)
                            var length = ze.size

//                            var bufferSize = zis.read(buffer)
                            var bufferSize = 0
                            fout.use { fOut ->
                                while ({bufferSize = zis.read(buffer); bufferSize }() != -1) {
                                    if (!isRunning) {
                                        mStateController.onStateEnd(false)
                                        zis.closeEntry()
                                        return@subscribe
                                    }

                                    fOut.write(buffer, 0, bufferSize)

                                    val seek = if (length > bufferSize) {
                                        bufferSize
                                    } else {
                                        length.toInt()
                                    }
                                    val currentTime = SystemClock.uptimeMillis()

                                    val nextSendProgressTime = lastTimeSendProgress + PROGRESS_PERIOD
                                    if (nextSendProgressTime > currentTime
                                        && totalBytesRemaining - mSeek > 0) {
                                        //Timber.d("nextSendProgressTime: $nextSendProgressTime, currentTime: $currentTime")
                                        mSeek += seek
                                        continue
                                    }

                                    mSeek += seek
                                    length -= mSeek.toLong()
                                    val timePassed = currentTime - lastTimeSendProgress
                                    lastTimeSendProgress = currentTime
                                    //Timber.d("lastTimeSendProgress change: $lastTimeSendProgress")

                                    if (timePassed > 0) {
                                        val currentSpeedSample = mSeek.toFloat() / timePassed.toFloat()
                                        if (0f != averageVerifySpeed) {
                                            averageVerifySpeed =
                                                SMOOTHING_FACTOR * currentSpeedSample + (1 - SMOOTHING_FACTOR) * averageVerifySpeed
                                        } else {
                                            averageVerifySpeed = currentSpeedSample
                                        }
                                        totalBytesRemaining -= mSeek.toLong()
                                        timeRemaining = (totalBytesRemaining / averageVerifySpeed).toLong()

                                        val progressInfo = DownloadProgressInfo(
                                            totalCompressedLength,
                                            totalCompressedLength - totalBytesRemaining,
                                            timeRemaining,
                                            averageVerifySpeed
                                        )
                                        onDownloadProgress(progressInfo)
                                    }
                                    mSeek = 0

                                    //Timber.d("buffer size: $bufferSize")
                                }
                            }
                            zis.closeEntry()
                        }
                        ze = zis.nextEntry
                    }
                    zis.close()
                } catch (e: Exception) {
                    Timber.log(Log.ERROR, "Decompress zip file failed, e: $e")
                    mStateController.onStateEnd(false)
                    return@subscribe
                }
                mStateController.onStateEnd(true)
            }, { throwable -> Timber.log(Log.ERROR, "onError, ${throwable.message}") })
    }

    private fun dirChecker(path: String?, dir: String) {
        val f = File(path + File.separator + dir)
        if (!f.isDirectory) {
            Timber.d("Creating new folder, path: ${f.absolutePath}")
            f.mkdirs()
        }
    }

    private fun checkInternalSoLibrary() {
        var success = true

        val soFile = File(DeviceUtils.getInternalSOFilePath())
        if (!soFile.exists()) {
            success = false
        }

        Timber.log(Log.DEBUG, "SO library file length: ${soFile.length()}")
        if (soFile.length() != DeviceUtils.FDR_SO_FILE_LENGTH) {
            success = false
        }

        mStateController.onStateEnd(success)
    }

    fun start() {
        Timber.d("start()")

        if (mStateController.getCurrentState() == OBB_STATE_DOWNLOAD_XAPKS && mRemoteService != null) {
            mRemoteService?.requestContinueDownload()
        } else {
            stop()
            mStateController.startFromState(OBB_STATE_CHECK_SO)
        }

        isRunning = true
    }

    fun pause() {
        Timber.d("pause()")

        if (mStateController.getCurrentState() == OBB_STATE_DOWNLOAD_XAPKS && mRemoteService != null) {
            mRemoteService?.requestPauseDownload()
        } else {
            mStateController.cancel()
            mCompositeDisposable.clear()
        }

        isRunning = false
    }

    fun stop() {
        Timber.d("stop()")

        mRemoteService?.requestPauseDownload()
        mDownloaderClientStub?.disconnect(mContext)

        mStateController.cancel()
        mCompositeDisposable.clear()

        val intent = Intent()
        intent.setClass(mContext, ObbDownloaderService::class.java)
        mActivity.stopService(intent)

        isRunning = false
    }

    override fun onDownloadStateChanged(newState: Int) {
        Timber.d("onDownloadStateChanged(), newState: $newState")

        if (mStateController.getCurrentState() != OBB_STATE_DOWNLOAD_XAPKS) {
            return
        }

        // notify subscriber
        downloadStateLiveEvent.postValue(newState)

        if (mDownloadState != newState) {
            mDownloadState = newState

            when (mDownloadState) {
                IDownloaderClient.STATE_COMPLETED -> mStateController.onStateEnd(true)

                IDownloaderClient.STATE_IDLE,
                IDownloaderClient.STATE_CONNECTING,
                IDownloaderClient.STATE_FETCHING_URL,
                IDownloaderClient.STATE_DOWNLOADING,
                IDownloaderClient.STATE_PAUSED_BY_REQUEST,
                IDownloaderClient.STATE_PAUSED_ROAMING,
                IDownloaderClient.STATE_PAUSED_SDCARD_UNAVAILABLE,
                IDownloaderClient.STATE_FAILED_CANCELED,
                IDownloaderClient.STATE_FAILED,
                IDownloaderClient.STATE_FAILED_FETCHING_URL,
                IDownloaderClient.STATE_FAILED_UNLICENSED -> { } // do nothing

                else -> mStateController.onStateEnd(false)
            }
        }
    }

    override fun onDownloadProgress(progress: DownloadProgressInfo?) {
        Timber.d("Progress, mOverallTotal: ${progress?.mOverallTotal}")
        Timber.d("Progress, mOverallProgress: ${progress?.mOverallProgress}")
        Timber.d("Progress, mTimeRemaining: ${progress?.mTimeRemaining}")
        Timber.d("Progress, speed: ${progress?.mCurrentSpeed}")
        curProgressLiveEvent.postValue(progress)
    }

    override fun onServiceConnected(m: Messenger?) {
        mRemoteService = DownloaderServiceMarshaller.CreateProxy(m)
        mRemoteService?.setDownloadFlags(DownloaderService.FLAGS_DOWNLOAD_OVER_CELLULAR)
        mRemoteService?.onClientUpdated(mDownloaderClientStub?.messenger)
    }
}