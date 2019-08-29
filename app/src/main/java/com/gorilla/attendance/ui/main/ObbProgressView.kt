package com.gorilla.attendance.ui.main

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.google.android.vending.expansion.downloader.DownloadProgressInfo
import com.google.android.vending.expansion.downloader.Helpers
import com.google.android.vending.expansion.downloader.IDownloaderClient
import com.gorilla.attendance.R
import com.gorilla.attendance.databinding.ObbProgressLayoutBinding
import com.gorilla.attendance.ui.common.WaitingProgressView
import com.gorilla.attendance.utils.ObbManager
import com.gorilla.attendance.utils.SimpleRxTask
import com.gorilla.attendance.utils.SingleLiveEvent
import com.jakewharton.rxbinding.view.RxView
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/6/28
 * Description:
 */
class ObbProgressView(context: Context, attrs: AttributeSet): WaitingProgressView(context, attrs) {

    companion object {
        const val STATE_PAUSE = 0
        const val STATE_RESUME = 1
        const val STATE_RETRY = 2
    }

    val progressStateLiveEvent = SingleLiveEvent<Int>()

    private var mStatePaused = false
    private var mBinding: ObbProgressLayoutBinding? = null

    init {
        initView(context)
    }

    override fun initView(context: Context) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        //inflater.inflate(R.layout.obb_progress_layout, this)
        mBinding = ObbProgressLayoutBinding.inflate(inflater, this, true)

        mBinding?.pauseBtn?.let {
            RxView.clicks(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .subscribe {
                    Timber.d("current pause STATE: $mStatePaused")
                    if (!mStatePaused) {
                        progressStateLiveEvent.postValue(STATE_PAUSE)
                    } else {
                        progressStateLiveEvent.postValue(STATE_RESUME)
                    }
                    setButtonPausedState(!mStatePaused)
                }
        }

        mBinding?.retryBtn?.let {
            RxView.clicks(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .subscribe {
                    progressStateLiveEvent.postValue(STATE_RETRY)
                    mBinding?.retryBtn?.visibility = View.GONE
                    mBinding?.pauseBtn?.visibility = View.VISIBLE
                }
        }
    }

    fun onDownLoadState(state: Int?) {
        val paused: Boolean
        val indeterminate: Boolean
        when (state) {
            IDownloaderClient.STATE_IDLE -> {
                // STATE_IDLE means the service is listening, so it's
                // safe to start making calls via mRemoteService.
                paused = false
                indeterminate = true
            }
            IDownloaderClient.STATE_CONNECTING, IDownloaderClient.STATE_FETCHING_URL -> {
                paused = false
                indeterminate = true
            }
            IDownloaderClient.STATE_DOWNLOADING -> {
                paused = false
                indeterminate = false
            }

            IDownloaderClient.STATE_FAILED_CANCELED, IDownloaderClient.STATE_FAILED, IDownloaderClient.STATE_FAILED_FETCHING_URL, IDownloaderClient.STATE_FAILED_UNLICENSED -> {
                paused = true
                indeterminate = false
            }
            IDownloaderClient.STATE_PAUSED_NEED_CELLULAR_PERMISSION, IDownloaderClient.STATE_PAUSED_WIFI_DISABLED_NEED_CELLULAR_PERMISSION -> {
                paused = true
                indeterminate = false
            }

            IDownloaderClient.STATE_PAUSED_BY_REQUEST -> {
                paused = true
                indeterminate = false
            }
            IDownloaderClient.STATE_PAUSED_ROAMING, IDownloaderClient.STATE_PAUSED_SDCARD_UNAVAILABLE -> {
                paused = true
                indeterminate = false
            }
            IDownloaderClient.STATE_COMPLETED -> {
                paused = false
                indeterminate = false
            }
            else -> {
                paused = true
                indeterminate = true
            }
        }

        setButtonPausedState(paused)
        if (state != null ) {
            mBinding?.statusText?.text = context.getText(Helpers.getDownloaderStringResourceIDFromState(state))
            mBinding?.progressBar?.isIndeterminate = indeterminate
        }
    }

    private fun setButtonPausedState(paused: Boolean) {
        SimpleRxTask.onMain {
            mStatePaused = paused
            mBinding?.pauseBtn?.text = if (paused) {
                context.getString(R.string.resume)
            } else {
                context.getString(R.string.pause)
            }
        }
    }

    fun onProgress(progressInfo: DownloadProgressInfo?) {
        mBinding?.progressAverageSpeed?.text = context.getString(
            R.string.kilobytes_per_second,
            Helpers.getSpeedString(progressInfo?.mCurrentSpeed ?: 0.0F)
        )

        mBinding?.progressTimeRemaining?.text = context.getString(
            R.string.time_remaining,
            Helpers.getTimeRemaining(progressInfo?.mTimeRemaining ?: 0L)
        )

        val total = progressInfo?.mOverallTotal ?: 1L
        val progress = progressInfo?.mOverallProgress ?: 1L

        mBinding?.progressBar?.max = total.shr(8).toInt()
        mBinding?.progressValue = progress.toInt().shr(8)

        val percent = (progress * 100 / total).toString() + "%"
        Timber.d("onProgress(), percent: $percent")

        mBinding?.progressAsPercentage?.text = percent
        mBinding?.progressAsFraction?.text = Helpers.getDownloadProgressString(
            progress, total
        )
    }

    fun onState(state: Int?) {
        val textRes = when (state) {
            ObbManager.OBB_STATE_DOWNLOAD_XAPKS -> R.string.status_download_Obb
            ObbManager.OBB_STATE_CHECK_DOWNLOAD_XAPKS -> R.string.status_check_obb
            ObbManager.OBB_STATE_UNZIP_OBB -> R.string.status_unzip_obb
            ObbManager.OBB_STATE_CHECK_SO -> R.string.status_check_so
            else -> return
        }

        SimpleRxTask.onMain {
            mBinding?.pauseBtn?.visibility = View.VISIBLE
            mBinding?.retryBtn?.visibility = View.GONE
            mBinding?.statusText?.text = context.getString(textRes)
            mBinding?.progressBar?.isIndeterminate = false
        }
    }


    fun onComplete(success: Boolean?) {
        if (success == true) {
            val progressInfo = DownloadProgressInfo(
                100,
                100,
                0,
                0F
            )
            onProgress(progressInfo)
            mBinding?.statusText?.text = context.getString(R.string.status_download_success)
        } else {
            mBinding?.statusText?.text = context.getString(R.string.status_download_failed)
            mBinding?.pauseBtn?.visibility = View.GONE
            mBinding?.retryBtn?.visibility = View.VISIBLE
        }
    }
}