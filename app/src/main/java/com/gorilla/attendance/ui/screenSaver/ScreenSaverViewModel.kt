package com.gorilla.attendance.ui.screenSaver

import androidx.lifecycle.ViewModel
import com.gorilla.attendance.data.model.NetworkState
import com.gorilla.attendance.ui.common.SharedViewModel
import com.gorilla.attendance.utils.DeviceUtils
import com.gorilla.attendance.utils.SingleLiveEvent
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.ArrayList

@Singleton
class ScreenSaverViewModel @Inject constructor(
    sharedViewModel: SharedViewModel,
    compositeDisposable: CompositeDisposable
) : ViewModel() {

    private var mSharedViewModel: SharedViewModel = sharedViewModel
    private var mCompositeDisposable : CompositeDisposable = compositeDisposable

    var initialLoad = SingleLiveEvent<NetworkState>()

    val dateTimeData = SingleLiveEvent<ArrayList<String>>()

    val stopVideoEvent = SingleLiveEvent<Boolean>()

    val syncMarqueesEvent = SingleLiveEvent<Boolean>()
    val syncVideosEvent = SingleLiveEvent<Boolean>()

    private var videoIndex = 0

    fun isAllVideosExist(): Boolean {
        if (DeviceUtils.deviceVideos == null || DeviceUtils.deviceVideos?.size == 0) {
            Timber.d("DeviceUtils.deviceVideos?.size == 0")
            return false
        }

        for (video in DeviceUtils.deviceVideos ?: return false) {
            val file = File(DeviceUtils.SD_CARD_APP_CONTENT + "/" + video.name)
            if (!file.exists()) {
                Timber.d("File is not exist, name = ${file.name}")
                return false
            }
            else if (file.length() != video.fileSize) {
                Timber.d("File size is not match...")
                return false
            }
        }

        return true
    }

    fun getVideoFilePath(): String? {
        if (!isAllVideosExist()) {
            videoIndex = 0
            return null
        }

        try {
            DeviceUtils.deviceVideos?.let {
                videoIndex %= it.size

                return DeviceUtils.SD_CARD_APP_CONTENT + "/" + it[videoIndex].name
            }
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
        }

        return null
    }

    fun addVideoIndex() {
        videoIndex++
    }

    fun updateDateTime() {
        //mDateTimeHandler.postDelayed(updateTimerThread, DeviceUtils.TIMER_DELAYED_TIME)
        Completable.complete()
            .delay(DeviceUtils.TIMER_DELAYED_TIME, TimeUnit.MILLISECONDS)
            .doOnComplete {
                //Timber.d("Update date time")

                val listResult = ArrayList<String>()
                val now = Calendar.getInstance().time

                // Time
                var sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                listResult.add(sdf.format(now))

                // Date
                sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                listResult.add(sdf.format(now))

                // Day of week
                sdf = SimpleDateFormat("EEE", Locale.getDefault())
                listResult.add(sdf.format(now))

                dateTimeData.postValue(listResult)

                // continue to update date time
                updateDateTime()
            }
            .subscribe()
    }

    override fun onCleared() {
        super.onCleared()
        mCompositeDisposable.clear()
    }

}