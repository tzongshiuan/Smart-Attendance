package com.gorilla.attendance.utils

import android.content.Context
import android.graphics.Color
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

        private const val MAX_OCCUR_NUM = 3

        //private const val MAX_FACE_DETECTION = 1
        //private const val MAX_LIVENESS_FAIL = 3

        const val STATUS_DEFAULT = -1
        const val STATUS_IDENTIFYING_FACE = 0
        const val STATUS_FACE_FORWARD_CAMERA = 1
        const val STATUS_GET_FACE_FAILED = 2
        const val STATUS_GET_FACE_SUCCESS = 3
        const val STATUS_GET_FACE_OCCUR = 4
        const val STATUS_GET_FACE_TIMEOUT = 5

        const val FACE_DETECT_COLOR_NORMAL  = 0x7FFFFFFF
        const val FACE_DETECT_COLOR_VALID   = Color.GREEN    // means verified success
        const val FACE_DETECT_COLOR_INVALID = Color.BLUE    // means verified failed
        const val FACE_DETECT_COLOR_FAILED  = Color.RED    // means occlusive / non-live face
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

    private var occurNum = 0

    fun restartScan() {
        mIsOnPause = false
        changeFaceColor(FACE_DETECT_COLOR_NORMAL)
        occurNum = MAX_OCCUR_NUM
    }

    fun changeFaceColor(color: Int) {
        mFdrCtrl?.setFaceViewColor(color)
    }

    fun initFdr(activity: AppCompatActivity, factory: AttendanceViewModelFactory) {
        sharedViewModel = ViewModelProviders.of(activity, factory).get(SharedViewModel::class.java)

        Timber.d("init FDR, controller = $mFdrCtrl")
        if (mFdrCtrl != null) {
            Timber.d("controller had already been initialized")
//            mFdrCtrl?.stopIdentify()
//            mFdrCtrl?.logoutFDRService()
//            mFdrCtrl?.release()
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

        mFdrCtrl?.setIODLivenessEnable(mPreferences.isEnableHumanDetection)
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

        if (mPreferences.faceVerifyTime == 0L) {
            return
        }

        countDownDisposoble = RxCountDownTimer.create(mPreferences.faceVerifyTime, 500)
            .subscribe {millisUntilFinished ->
                Timber.i("CountDown: $millisUntilFinished (milli-secs)")

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

    fun stopVerifyCountDown() {
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
//            if (!isForQrCode) {
//                if (mPreferences.faceVerifyTime != 0L) {
//                    // start countdown
//                    isVerifyTimeout = false
//                    startVerifyCountDown()
//                }
//            }
            return
        }
        isRunning = true

        Timber.d("startFdr(), isForQrCode: $isForQrCode")

        occurNum = MAX_OCCUR_NUM

        if (!isForQrCode) {
            if (mPreferences.faceVerifyTime != 0L) {
                // start countdown
                isVerifyTimeout = false
                startVerifyCountDown()
            }

            mIsOnPause = false
            mFdrCtrl?.hideFaceView(false)
            changeFaceColor(FACE_DETECT_COLOR_NORMAL)
            mFdrCtrl?.startFaceDetection()
            mFdrCtrl?.setHideFaceDetectBox(false)
        } else {
            mIsOnPause = true
            mFdrCtrl?.hideFaceView(false)
            mFdrCtrl?.startFaceDetection()
            mFdrCtrl?.setHideFaceDetectBox(true)
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

        if (!isForQrCode) {
            stopVerifyCountDown()
            mFdrCtrl?.stopFaceDetection()
            fdrMainEvent.postValue(STATUS_DEFAULT)
            fdrStatusLiveData.postValue(STATUS_DEFAULT)
        }

        if (mPreferences.webcamType == Constants.RTSP_WEB_CAM) {
            mFdrCtrl?.disconnectRTSP()
        } else {
            mFdrCtrl?.releaseCamera(CAMERA_ID)
        }

        isRunning = false

        mIsOnPause = true
    }

    private fun setFdrRange(fdrRange: Int) {
        Timber.d("setFdrRange, range = $fdrRange")

        val widthPx = mContext.resources.getDimension(R.dimen.fdr_width)
        val heightPx = mContext.resources.getDimension(R.dimen.fdr_height)
        val widthDp = (widthPx / mContext.resources.displayMetrics.density).toInt()
        val heightDp = (heightPx / mContext.resources.displayMetrics.density).toInt()

        Timber.i("widthPx = $widthPx")
        Timber.i("heightPx = $heightPx")
        Timber.i("widthDp = $widthDp")
        Timber.i("heightDp = $heightDp")

        val topX = widthDp * (((100.0 - fdrRange) / 2).toFloat() / 100)
        val topY = heightDp * (((100.0 - fdrRange) / 2).toFloat() / 100)
        val bottomX = widthDp.toFloat() - topX
        val bottomY = heightDp.toFloat() - topY

        Timber.i("topX = $topX")
        Timber.i("topY = $topY")
        Timber.i("bottomX = $bottomX")
        Timber.i("bottomY = $bottomY")

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
            Timber.d("onIODTriggerResponse trigger = $trigger")
            Timber.i("onIODTriggerResponse iodInfo.width = %s", iodInfo.width)
            Timber.i("onIODTriggerResponse iodInfo.height = %s", iodInfo.height)
            Timber.i("onIODTriggerResponse iodInfo.x = %s", iodInfo.x)
            Timber.i("onIODTriggerResponse iodInfo.y = %s", iodInfo.y)
            Timber.i("onIODTriggerResponse iodInfo.liveness_result = %s", iodInfo.liveness_result)

            /**
             * liveness_result
             * 活體
             * 0: NO_RESULT
             * 1: SPOOF
             * 2: UNKNOWN
             * 3: REAL
             */
            // about video fragment
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
                Timber.i("mFaceDetectThread start r = $r")

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

                // 註冊時略過被判斷為遮蔽與非活體的人臉物件
                val applicationMode = mPreferences.applicationMode

                if (r != 0) {
                    Timber.d("mFaceDetectThread mPreferences.isEnableHumanDetection = %s", mPreferences.isEnableHumanDetection)
                    Timber.d("mFaceDetectThread iodfeature.frSuitableType = %s", iodFeature.frSuitableType)
                    Timber.d("mFaceDetectThread iodfeature.occlusionType = %s", iodFeature.occlusionType)

                    if (iodFeature.occlusionType == IntelligentObjectDetector.Face_Occlusion_Type.GORILLA_FACE_OCCLUSION_UNKNOWN
                        || iodFeature.occlusionType == IntelligentObjectDetector.Face_Occlusion_Type.GORILLA_FACE_OCCLUSION_DETECTED) {
                        //mask on face
                        if (applicationMode == Constants.VERIFICATION_MODE) {
                            if (occurNum != 0) {
                                occurNum--
                            } else {
                                synchronized(isVerifyTimeout) {
                                    if (!mIsOnPause && !isVerifyTimeout) {
                                        // stop count down
                                        stopVerifyCountDown()
                                        fdrMainEvent.postValue(STATUS_GET_FACE_OCCUR)
                                        fdrStatusLiveData.postValue(STATUS_GET_FACE_OCCUR)
                                        changeFaceColor(FACE_DETECT_COLOR_FAILED)
                                        mIsOnPause = true
                                    }
                                }
                            }
                        } else {
                            changeFaceColor(FACE_DETECT_COLOR_FAILED)
                        }

                        return@IODTriggerResponseListener
                    }

                    if (iodFeature.frSuitableType == IntelligentObjectDetector.FR_Suitable_Type.GORILLA_FR_NOT_SUITABLE) {
                        return@IODTriggerResponseListener
                    }

                    if (mPreferences.isEnableHumanDetection) {
                        Timber.d("mFaceDetectThread start iodfeature.liveness = %s", iodFeature.liveness)
                        if (iodFeature.liveness == IntelligentObjectDetector.Liveness_Type.IOD_LIVENESS_REAL) {
                            for (i in currentPngList.indices) {
                                if (i == currentPngList.size - 1) {
                                    DeviceUtils.mFacePngList.add(currentPngList[i])
                                }
                            }

//                            if (mLivenessCount >= MAX_LIVENESS_FAIL) {
//                                sharedViewModel.clockData.liveness = Constants.LIVENESS_FAILED
//                            } else {
                                sharedViewModel.clockData.liveness = Constants.LIVENESS_SUCCEED
//                            }

                            mLivenessCount = 0
                        } else {
                            //for (i in currentPngList.indices) {
                            //    if (i == currentPngList.size - 1) {
                            //        DeviceUtils.mFacePngList.add(currentPngList[i])
                            //    }
                            //}
                            //mLivenessCount++;
                            sharedViewModel.clockData.liveness = Constants.LIVENESS_FAILED

                            if (applicationMode == Constants.VERIFICATION_MODE) {
                                synchronized(isVerifyTimeout) {
                                    if (!mIsOnPause && !isVerifyTimeout) {
                                        // stop count down
                                        stopVerifyCountDown()
                                        fdrMainEvent.postValue(STATUS_GET_FACE_FAILED)
                                        fdrStatusLiveData.postValue(STATUS_GET_FACE_FAILED)
                                        changeFaceColor(FACE_DETECT_COLOR_FAILED)
                                        mIsOnPause = true
                                    }
                                }
                            } else {
                                changeFaceColor(FACE_DETECT_COLOR_FAILED)
                            }
                            return@IODTriggerResponseListener
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
            } else {
                // show face forward
                //fdrMainEvent.postValue(STATUS_FACE_FORWARD_CAMERA)
                //fdrStatusLiveData.postValue(STATUS_FACE_FORWARD_CAMERA)
            }
        }

    private val camPreviewCallback = object: MyFDRControl.cameraPreviewCallback {
        override fun onCameraStarted() {
            Timber.i("onCameraStarted()")

            Timber.i("onCameraStarted %s", mFdrCtrl?.maxExposure)
            Timber.i("onCameraStarted %s", mFdrCtrl?.minExposure)
            Timber.i("onCameraStarted %s", mFdrCtrl?.exposure)
            Timber.i("onCameraStarted %s", mFdrCtrl?.exposureStep)
            Timber.i("onCameraStarted %s", mFdrCtrl?.exposure)
            //fdrCtrl.setExposure(10);
        }

        override fun onCameraPreview() {
            //Timber.d("onCameraPreview()")
            //fdrCtrl.setExposure(-10);
        }
    }
}