package com.gorilla.attendance.utils

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import com.gorilla.attendance.R
import com.gorilla.attendance.data.model.SingletonHolder
import com.gorilla.attendance.ui.common.SharedViewModel
import com.gorilla.attendance.ui.main.MainActivity
import com.gorilla.attendance.utils.rxCountDownTimer.RxCountDownTimer
import com.gorilla.attendance.viewModel.AttendanceViewModelFactory
import com.gorillatechnology.fdrcontrol.FDRControl
import gorilla.iod.IntelligentObjectDetector
import io.reactivex.disposables.Disposable
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/7/1
 * Description:
 */
class FdrManager @Inject constructor(context: Context, preferences: PreferencesHelper) {

    private lateinit var sharedViewModel: SharedViewModel

    private val mContext= context
    private val mPreferences = preferences
    private var mActivity: AppCompatActivity? = null
    private var mFactory: AttendanceViewModelFactory? = null

    companion object : SingletonHolder<FdrManager, Context, PreferencesHelper>(::FdrManager) {
        // if no need any argument, just use lazy method
        //var instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { FdrManager() }

        const val CAMERA_ID = 1

        private const val IOD_OBJ_WIDTH_MIN = 150
        private const val IOD_OBJ_WIDTH_MAX = 1000
        private const val IOD_OBJ_WIDTH_MIN_BEST = 150
        private const val IOD_OBJ_WIDTH_MAX_BEST = 900

        private const val SAMPLING_NUM = 3
        private const val SAMPLING_TIME = 1000
        private const val MAX_IMAGE_WIDTH = 320

        //private const val MAX_FACE_DETECTION = 1
        private const val MAX_LIVENESS_FAIL = 3

        const val STATUS_DEFAULT = -1
        const val STATUS_IDENTIFYING_FACE = 0
        const val STATUS_FACE_FORWARD_CAMERA = 1
        const val STATUS_GET_FACE_FAILED = 2
        const val STATUS_GET_FACE_SUCCESS = 3
        const val STATUS_GET_FACE_TIMEOUT = 4
    }

    val fdrMainEvent = SingleLiveEvent<Int>()
    val fdrStatusLiveData = SingleLiveEvent<Int>()

    var mFdrCtrl: MyFDRControl? = null

    private var mMode = FDRControl.Mode.RECOGNIZE
    private var mFaceSource = FDRControl.FaceSource.CAMERA

    private var mIsOnPause = false
    private var mLivenessCount = 0

    private var isRunning = false

    private var isVerifyTimeout = false

    private var countDownDisposoble: Disposable? = null

//    fun resetFdr() {
//        if (mActivity != null && mFactory != null) {
//            initFdr(mActivity!!, mFactory!!)
//        }
//    }

    fun initFdr(activity: AppCompatActivity, factory: AttendanceViewModelFactory) {
        sharedViewModel = ViewModelProviders.of(activity, factory).get(SharedViewModel::class.java)

        Timber.d("init FDR, controller = $mFdrCtrl")
        if (mFdrCtrl != null) {
            Timber.d("controller had already been initialized")
            mFdrCtrl = null
        }

        mActivity = activity
        mFactory = factory

        val libPath = DeviceUtils.APP_INTERNAL_BIN_FOLDER
        Timber.d("libPath = $libPath")

        when (mPreferences.webcamType) {
            Constants.ANDROID_BUILD_IN_LENS -> {
                Timber.d("Camera type: ANDROID_BUILD_IN_LENS")

                mFaceSource = FDRControl.FaceSource.CAMERA
                mFdrCtrl = MyFDRControl(mActivity, mMode, mFaceSource, CAMERA_ID, 0, libPath)
            }

            Constants.RTSP_WEB_CAM -> {
                Timber.d("Camera type: RTSP_WEB_CAM")
                Timber.d("mPreferences.webcamUrl = ${mPreferences.webcamUrl}")
                Timber.d("mPreferences.webcamUsername = ${mPreferences.webcamUsername}")
                Timber.d("mPreferences.webcamPassword = ${mPreferences.webcamPassword}")

                mFaceSource = FDRControl.FaceSource.RTSP
                mFdrCtrl = MyFDRControl(mActivity, mMode, mFaceSource, CAMERA_ID, 0, libPath).also {
                    it.setRTSPStatusListener(rtspListener)
                }
            }
        }

        mFdrCtrl?.setIODTriggerResponseListener(iodTriggerResponseListener)
        mFdrCtrl?.setCameraPreviewCallback(camPreviewCallback)

        mFdrCtrl?.setIODLivenessEnable(true)
        mFdrCtrl?.setIODObjWidth(IOD_OBJ_WIDTH_MIN, IOD_OBJ_WIDTH_MAX,
            IOD_OBJ_WIDTH_MIN_BEST, IOD_OBJ_WIDTH_MAX_BEST)

        // set FD range
        setFdrRange(DeviceUtils.mFdrRange)
        mFdrCtrl?.setIODFRSuitableEnable(true)
        mFdrCtrl?.setIODOcclusionEnable(true)

        mFdrCtrl?.setFaceListParam(SAMPLING_NUM, SAMPLING_TIME, FDRControl.CaptureMode.ByQuality, MAX_IMAGE_WIDTH)
    }

    private fun startVerifyCountDown() {
        if (sharedViewModel.isSingleModuleMode()) {
            return
        }

        countDownDisposoble = RxCountDownTimer.create(mPreferences.faceVerifyTime, 500)
            .subscribe {millisUntilFinished ->
                Timber.d("CountDown: $millisUntilFinished (milli-secs)")

                if (millisUntilFinished == 0L) {
                    synchronized(isVerifyTimeout) {
                        if (!mIsOnPause) {
                            isVerifyTimeout = true
                            fdrMainEvent.postValue(STATUS_GET_FACE_TIMEOUT)
                            fdrStatusLiveData.postValue(STATUS_GET_FACE_TIMEOUT)
                            mIsOnPause = true
                        }
                    }
                }
            }
    }

    private fun stopVerifyCountDown() {
        countDownDisposoble?.dispose()
        countDownDisposoble = null
        isVerifyTimeout = false
    }

    fun startFdr(isForQrCode: Boolean = false) {
        if (MainActivity.IS_SKIP_FDR) {
            fdrMainEvent.postValue(STATUS_GET_FACE_SUCCESS)
            fdrStatusLiveData.postValue(STATUS_GET_FACE_SUCCESS)
            return
        }

        if (isRunning) {
            return
        }
        isRunning = true

        Timber.d("startFdr()")
        mIsOnPause = false

        if (!isForQrCode) {
            if (mPreferences.faceVerifyTime != 0L) {
                // start countdown
                isVerifyTimeout = false
                startVerifyCountDown()
            }

            mFdrCtrl?.hideFaceView(false)
            mFdrCtrl?.startFaceDetection()
        } else {
            mFdrCtrl?.startFaceDetection()

            SimpleRxTask.afterOnMain(1000L) {
                mFdrCtrl?.stopFaceDetection()
            }
        }

        if (mPreferences.webcamType == Constants.RTSP_WEB_CAM) {
            Timber.d("mPreferences.webcamUrl = ${mPreferences.webcamUrl}")
            Timber.d("mPreferences.webcamUsername = ${mPreferences.webcamUsername}")
            Timber.d("mPreferences.webcamPassword = ${mPreferences.webcamPassword}")


            mFdrCtrl?.connectRTSP(
                DeviceUtils.getValidRtspUrl(mPreferences.webcamUrl),
                mPreferences.webcamUsername,
                mPreferences.webcamPassword
            )
        }
    }

    fun stopFdr(isForQrCode: Boolean = false) {
        if (!isRunning) {
            return
        }
        Timber.d("stopFdr()")

        mIsOnPause = true

        if (!isForQrCode) {
            stopVerifyCountDown()
            mFdrCtrl?.stopFaceDetection()
            fdrMainEvent.postValue(STATUS_DEFAULT)
            fdrStatusLiveData.postValue(STATUS_DEFAULT)
        }

        mFdrCtrl?.releaseCamera(CAMERA_ID)


        if (mPreferences.webcamType == Constants.RTSP_WEB_CAM) {
            mFdrCtrl?.disconnectRTSP()
            //mFdrCtrl?.release()
        }

        isRunning = false
    }

    private fun setFdrRange(fdrRange: Int) {
        Timber.d("setFdrRange, range = $fdrRange")

        val widthPx = mContext.resources.getDimension(R.dimen.fdr_width)
        val heightPx = mContext.resources.getDimension(R.dimen.fdr_height)
        val widthDp = (widthPx / mContext.resources.displayMetrics.density).toInt()
        val heightDp = (heightPx / mContext.resources.displayMetrics.density).toInt()

        Timber.d("widthPx = $widthPx")
        Timber.d("heightPx = $heightPx")
        Timber.d("widthDp = $widthDp")
        Timber.d("heightDp = $heightDp")

        val topX = widthDp * (((100.0 - fdrRange) / 2).toFloat() / 100)
        val topY = heightDp * (((100.0 - fdrRange) / 2).toFloat() / 100)
        val bottomX = widthDp.toFloat() - topX
        val bottomY = heightDp.toFloat() - topY

        Timber.d("topX = $topX")
        Timber.d("topY = $topY")
        Timber.d("bottomX = $bottomX")
        Timber.d("bottomY = $bottomY")

        mFdrCtrl?.setBestObjLoc(topX, topY, bottomX, bottomY)
    }

    private val rtspListener = object: MyFDRControl.RTSPStatusListener {
        override fun onClosed() {
        }

        override fun onStoped() {
        }

        override fun onConnecting() {
        }

        override fun onPlaying() {
        }

        override fun onWaitRetry() {
        }

        override fun onError(code: Int) {
        }
    }

    private val iodTriggerResponseListener =
        MyFDRControl.IODTriggerResponseListener { trigger, iodInfo ->
            Timber.d("onIODTriggerResponse11 trigger = $trigger")
            Timber.d("onIODTriggerResponse iodInfo.width = %s", iodInfo.width)
            Timber.d("onIODTriggerResponse iodInfo.height = %s", iodInfo.height)
            Timber.d("onIODTriggerResponse iodInfo.x = %s", iodInfo.x)
            Timber.d("onIODTriggerResponse iodInfo.y = %s", iodInfo.y)
            Timber.d("onIODTriggerResponse iodInfo.liveness_result = %s", iodInfo.liveness_result)

            /**
             * liveness_result
             * 活體
             * 0: NO_RESULT
             * 1: SPOOF
             * 2: UNKNOWN
             * 3: REAL
             */
            // TODO about video fragment
//            Timber.d("mMainActivity.getSupportFragmentManager().findFragmentByTag(VideoFragment.TAG) = "
//                    + mMainActivity.getSupportFragmentManager().findFragmentByTag(VideoFragment.TAG)
//            )
//            if (mMainActivity.getSupportFragmentManager().findFragmentByTag(VideoFragment.TAG) != null) {
//                val videoFragment =
//                    mMainActivity.getSupportFragmentManager().findFragmentByTag(VideoFragment.TAG) as VideoFragment
//                videoFragment.closeVideo()
//                return@IODTriggerResponseListener
//            }

            if (trigger == MyFDRControl.TriggerType.DETERMINED) {
                //show Identifying
                fdrMainEvent.postValue(STATUS_IDENTIFYING_FACE)
                fdrStatusLiveData.postValue(STATUS_IDENTIFYING_FACE)

                val iodFeature = FDRControl.IODfeature()

                val currentPngList = ArrayList<ByteArray>()
                val iniList = ArrayList<String>()

                val r = mFdrCtrl?.getRecognizedFaceList(currentPngList, iniList, iodFeature)
                Timber.d("mFaceDetectThread start r = $r")

                //GG TEST DATA, test live fail
//                if (r != 0){
//                    for(int i = 0 ; i < currentPngList.size() ; i++){
//                        if(i == (currentPngList.size() - 1)){
//                            EnterpriseUtils.mFacePngList.add(currentPngList.get(i));
//                        }
//                    }
//                //                        mLivenessCount++;
//                    ClockUtils.mLiveness = Constants.LIVENESS_FAILED;
//
//                    mActivityCallbackHandler.sendEmptyMessage(Constants.GET_FACE_FAIL);
//                    mFragmentCallbackHandler.sendEmptyMessage(Constants.GET_FACE_FAIL);
//                    mIsOnPause = true;
//                    return;
//                }

                if (r != 0) {
                    Timber.d("mFaceDetectThread DeviceUtils.mIsLivenessOn = %s", DeviceUtils.mIsLivenessOn)
                    Timber.d("mFaceDetectThread iodfeature.frSuitableType = %s", iodFeature.frSuitableType)
                    Timber.d("mFaceDetectThread iodfeature.occlusionType = %s", iodFeature.occlusionType)

                    if (iodFeature.occlusionType == IntelligentObjectDetector.Face_Occlusion_Type.GORILLA_FACE_OCCLUSION_UNKNOWN
                        || iodFeature.occlusionType == IntelligentObjectDetector.Face_Occlusion_Type.GORILLA_FACE_OCCLUSION_DETECTED) {
                        //mask on face
                        return@IODTriggerResponseListener
                    }

                    if (DeviceUtils.mIsLivenessOn) {
                        Timber.d("mFaceDetectThread start iodfeature.liveness = %s", iodFeature.liveness)
                        if (iodFeature.liveness == IntelligentObjectDetector.Liveness_Type.IOD_LIVENESS_REAL) {
                            for (i in currentPngList.indices) {
                                if (i == currentPngList.size - 1) {
                                    DeviceUtils.mFacePngList.add(currentPngList[i])
                                }
                            }

                            if (mLivenessCount >= MAX_LIVENESS_FAIL) {
                                sharedViewModel.clockData.liveness = Constants.LIVENESS_FAILED
                            } else {
                                sharedViewModel.clockData.liveness = Constants.LIVENESS_SUCCEED
                            }

                            mLivenessCount = 0
                        } else {
                            for (i in currentPngList.indices) {
                                if (i == currentPngList.size - 1) {
                                    DeviceUtils.mFacePngList.add(currentPngList[i])
                                }
                            }
                            //mLivenessCount++;
                            sharedViewModel.clockData.liveness = Constants.LIVENESS_FAILED

                            fdrMainEvent.postValue(STATUS_GET_FACE_FAILED)
                            fdrStatusLiveData.postValue(STATUS_GET_FACE_FAILED)
                            mIsOnPause = true
                        }
                    } else {
                        for (i in currentPngList.indices) {
                            if (i == currentPngList.size - 1) {
                                DeviceUtils.mFacePngList.add(currentPngList[i])
                            }
                        }
                        mLivenessCount = 0
                        sharedViewModel.clockData.liveness = Constants.LIVENESS_OFF

//                        if(mLivenessCount >= MAX_LIVENESS_FAIL){
//                            ClockUtils.mLiveness = "FAILED";
//                        }else{
//                            ClockUtils.mLiveness = "SUCCEED";
//                        }
                    }

                    Timber.d("mFaceDetectThread start mIsOnPause = $mIsOnPause")
                    synchronized(isVerifyTimeout) {
                        if (!mIsOnPause && DeviceUtils.mFacePngList.size > 0 && !isVerifyTimeout) {
                            // stop count down
                            stopVerifyCountDown()

                            fdrMainEvent.postValue(STATUS_GET_FACE_SUCCESS)
                            fdrStatusLiveData.postValue(STATUS_GET_FACE_SUCCESS)
                            mIsOnPause = true
                        }
                    }
                } else {
                    //no get face
                }
                //end determine
            } else {
                // show face forward
                fdrMainEvent.postValue(STATUS_FACE_FORWARD_CAMERA)
                fdrStatusLiveData.postValue(STATUS_FACE_FORWARD_CAMERA)
            }
        }

    private val camPreviewCallback = object: MyFDRControl.cameraPreviewCallback {
        override fun onCameraStarted() {
            Timber.d("onCameraStarted()")

            Timber.d("onCameraStarted %s", mFdrCtrl?.maxExposure)
            Timber.d("onCameraStarted %s", mFdrCtrl?.minExposure)
            Timber.d("onCameraStarted %s", mFdrCtrl?.exposure)
            Timber.d("onCameraStarted %s", mFdrCtrl?.exposureStep)
            Timber.d("onCameraStarted %s", mFdrCtrl?.exposure)
            //fdrCtrl.setExposure(10);
        }

        override fun onCameraPreview() {
            //Timber.d("onCameraPreview()")
            //fdrCtrl.setExposure(-10);
        }
    }
}