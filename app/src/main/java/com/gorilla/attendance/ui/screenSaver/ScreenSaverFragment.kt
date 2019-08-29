package com.gorilla.attendance.ui.screenSaver

import android.media.MediaPlayer
import android.os.Bundle
import android.view.*
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.gorilla.attendance.R
import com.gorilla.attendance.databinding.ScreenSaverFragmentBinding
import com.gorilla.attendance.di.Injectable
import com.gorilla.attendance.ui.common.BaseFragment
import com.gorilla.attendance.ui.common.FootBarBaseInterface
import com.gorilla.attendance.ui.main.MainActivity
import com.gorilla.attendance.utils.Constants
import com.gorilla.attendance.utils.DeviceUtils
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar

class ScreenSaverFragment: BaseFragment(), FootBarBaseInterface, Injectable {

    companion object {
        const val SCREEN_SAVER_ALPHA = 38

        private var commonScreenSaverIndex = 0
        private val commonScreenSaverList = arrayListOf(
            R.mipmap.bg_screen_saver_1,
            R.mipmap.bg_screen_saver_2
        )
    }

    private var mBinding: ScreenSaverFragmentBinding? = null

    private lateinit var screenSaverViewModel: ScreenSaverViewModel

    private var mIsVideoPlaying = false

    private var mVideoFilePath: String? = null

    private var mMediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate()")

        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = ScreenSaverFragmentBinding.inflate(inflater, container, false)

        return mBinding?.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        screenSaverViewModel = ViewModelProviders.of(this, factory).get(ScreenSaverViewModel::class.java)

        mBinding?.viewModel = screenSaverViewModel

        initUI()

        initViewModelObservers()
    }

    override fun onStart() {
        super.onStart()
        Timber.d("onStart()")

        (activity as MainActivity).setToolbarVisible(false)
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume()")
    }

    override fun onStop() {
        super.onStop()
        Timber.d("onStop()")

        destroyMediaPlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy()")

        (activity as MainActivity).setToolbarVisible(true)
    }

    private fun initUI() {
        mVideoFilePath = screenSaverViewModel.getVideoFilePath()
        if (mVideoFilePath != null) {
            showVideoScreenSaverUI()
        } else {
            showCommonScreenSaverUI()
        }
    }

    private fun initViewModelObservers() {
        screenSaverViewModel.dateTimeData.observe(this, Observer { list ->
            if (list != null && list.size == 3) {
                mBinding?.timeText?.text = list[0]
                mBinding?.dateText?.text = list[1]
                mBinding?.weekDayText?.text = list[2]
            }
        })

        screenSaverViewModel.stopVideoEvent.observe(this, Observer { isStop ->
            if (isStop != null && isStop) {
                stopVideo()
            }
        })
    }

    private fun stopVideo() {
        Timber.d("stopVideo()")
//        if (!mIsVideoPlaying) {
//            Timber.d("Video hasn't played, mIsVideoPlaying = $mIsVideoPlaying")
//            return
//        }

        // stop video
        destroyMediaPlayer()

        showCommonScreenSaverUI()

        mIsVideoPlaying = false
    }

    /**
     * Play video as screen saver
     */
    private fun showVideoScreenSaverUI() {
        Timber.d("showVideoScreenSaverUI()")
        if (mIsVideoPlaying) {
            Timber.d("Video has been played, mIsVideoPlaying = $mIsVideoPlaying")
            return
        }

        mBinding?.commonGroup?.visibility = View.GONE
        mBinding?.videoLayout?.visibility = View.VISIBLE
        mIsVideoPlaying = true

        /**
         * Init media player
         */
        createMediaPlayer()

        /**
         * Init surface view
         */
        val layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)

        val surfaceView = SurfaceView(activity)
        surfaceView.layoutParams = layoutParams

        val surfaceHolder = surfaceView.holder
        surfaceHolder.setFixedSize(100, 100)
        surfaceHolder.addCallback(object: SurfaceHolder.Callback {
            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
                Timber.d("surfaceChanged()")
                Timber.d("holder = $holder")
                Timber.d("width = $width")
                Timber.d("height = $height")
                Timber.d("measure width = ${mBinding?.videoLayout?.measuredWidth}")
                Timber.d("measure height = ${mBinding?.videoLayout?.measuredHeight}")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {
                Timber.d("surfaceDestroyed()")
                mMediaPlayer?.setDisplay(null)
            }

            override fun surfaceCreated(holder: SurfaceHolder?) {
                Timber.d("surfaceCreated()")
                try {
                    mMediaPlayer?.setDisplay(surfaceHolder)
                } catch (e: Exception) {
                    Timber.e("set display holder failed, message: ${e.message}")
                }
            }
        })

        mBinding?.videoLayout?.addView(surfaceView)
    }

    /**
     * Common screen saver
     */
    private fun showCommonScreenSaverUI() {
        Timber.d("showCommonScreenSaverUI()")
        mBinding?.commonGroup?.visibility = View.VISIBLE
        mBinding?.videoLayout?.visibility = View.GONE

        val bg = ContextCompat.getDrawable(context!!, commonScreenSaverList[commonScreenSaverIndex++])
        bg?.let {
            it.alpha = SCREEN_SAVER_ALPHA
            mBinding?.screenSaverLayout?.background = it
        }
        commonScreenSaverIndex %= commonScreenSaverList.size

        val now = Calendar.getInstance().time
        // Time
        var sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        mBinding?.timeText?.text = sdf.format(now)
        // Date
        sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        mBinding?.dateText?.text = sdf.format(now)
        // Day of week
        sdf = SimpleDateFormat("EEE", Locale.getDefault())
        mBinding?.weekDayText?.text = sdf.format(now)
        screenSaverViewModel.updateDateTime()

        if (DeviceUtils.deviceMarquees?.size ?: 0 > 0) {
            val marquee = DeviceUtils.deviceMarquees?.get(0)
            mBinding?.marqueeText?.text =
                when (DeviceUtils.mLocale) {
                    Constants.LOCALE_TW -> marquee?.text?.zh_TW
                    Constants.LOCALE_EN -> marquee?.text?.en_US
                    Constants.LOCALE_CN -> marquee?.text?.zh_CN
                    else -> ""
                }
            mBinding?.marqueeText?.speed = marquee?.speed ?: 1

            mBinding?.marqueeText?.startScroll()
        }
    }

    private fun createMediaPlayer() {
        Timber.d("createMediaPlayer()")

        try {
            mMediaPlayer = MediaPlayer()
            mMediaPlayer?.setScreenOnWhilePlaying(true)
            mMediaPlayer?.setDataSource(mVideoFilePath)
            mMediaPlayer?.prepareAsync()

            // set listeners
            mMediaPlayer?.setOnPreparedListener { mediaPlayer ->
                if (mediaPlayer != null) {
                    Timber.d("onPreparedListener video width = ${mediaPlayer.videoWidth}")
                    Timber.d("onPreparedListener video height = ${mediaPlayer.videoHeight}")
                    mediaPlayer.start()
                }
            }

            mMediaPlayer?.setOnErrorListener { _, what, _ ->
                Timber.e("Error code: $what")
                stopVideo()
                false
            }

            mMediaPlayer?.setOnCompletionListener { mediaPlayer ->
                Timber.d("onCompletionListener()")

                try {
                    if (mediaPlayer != null) {
                        // play next video file
                        mediaPlayer.stop()
                        mediaPlayer.reset()
                        mVideoFilePath = screenSaverViewModel.getVideoFilePath()
                        mediaPlayer.setDataSource(mVideoFilePath)
                        mediaPlayer.prepareAsync()
                    }
                } catch (e: Exception) {
                    Timber.e("Play next video failed, message: ${e.message}")
                    stopVideo()
                }
            }
        } catch (e: Exception) {
            Timber.e("create media player failed, message: ${e.message}")
        }
    }

    private fun destroyMediaPlayer() {
        Timber.d("destroyMediaPlayer()")

        mMediaPlayer?.let {
            it.stop()
            it.reset()
            it.release()
        }
        mMediaPlayer = null
    }

    override fun changeFootBarUI() {
    }

    override fun onClickLeftBtn() {
    }

    override fun onClickRightBtn() {
    }
}