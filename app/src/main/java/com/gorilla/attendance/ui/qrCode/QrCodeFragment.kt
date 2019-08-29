package com.gorilla.attendance.ui.qrCode

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ResultPoint
import com.google.zxing.client.android.BeepManager
import com.google.zxing.client.android.Intents
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.gorilla.attendance.R
import com.gorilla.attendance.data.model.RegisterFormState
import com.gorilla.attendance.data.model.Status
import com.gorilla.attendance.databinding.QrCodeFragmentBinding
import com.gorilla.attendance.di.Injectable
import com.gorilla.attendance.ui.common.BaseFragment
import com.gorilla.attendance.ui.common.FootBarBaseInterface
import com.gorilla.attendance.ui.common.SharedViewModel
import com.gorilla.attendance.ui.main.MainActivity
import com.gorilla.attendance.utils.*
import com.gorilla.attendance.utils.rxCountDownTimer.RxCountDownTimer
import com.jakewharton.rxbinding.view.RxView
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import io.reactivex.disposables.Disposable
import rx.android.schedulers.AndroidSchedulers
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class QrCodeFragment: BaseFragment(), FootBarBaseInterface, Injectable {
    companion object {
        var isRtspClientRunning = false
    }

    private var mBinding: QrCodeFragmentBinding? = null

    private lateinit var qrCodeViewModel: QrCodeViewModel

    private lateinit var mBeepManager: BeepManager

    private var rtspDecodeThread: Thread? = null

    private var countDownDisposoble: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate()")

        setHasOptionsMenu(true)

        mBeepManager = BeepManager(activity)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = QrCodeFragmentBinding.inflate(inflater, container, false)

        return mBinding?.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        qrCodeViewModel = ViewModelProviders.of(this, factory).get(QrCodeViewModel::class.java)

        mBinding?.viewModel = qrCodeViewModel

        initUI()

        initViewModelObservers()
    }

    override fun onStart() {
        super.onStart()
        Timber.d("onStart()")
    }

    override fun onPause() {
        super.onPause()
        Timber.d("onPause()")

//        when (mPreferences.webcamType) {
//            Constants.ANDROID_BUILD_IN_LENS -> {
//                if (currentState == VERIFY_SECURITY_CODE_STATE) {
//                    mBinding?.barcodeScanner?.pauseAndWait()
//                }
//            }
//
//            Constants.RTSP_WEB_CAM -> {
//            }
//        }
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume()")

        if (mPreferences.applicationMode == Constants.VERIFICATION_MODE) {
            sharedViewModel.changeTitleEvent.postValue(getString(R.string.qr_verification_title))
        } else {
            when (sharedViewModel.clockModule) {
                SharedViewModel.MODULE_VISITOR -> sharedViewModel.changeTitleEvent.postValue(getString(R.string.visitor_registration_form))
                else -> sharedViewModel.changeTitleEvent.postValue(getString(R.string.employee_registration_form))
            }
        }

//        when (mPreferences.webcamType) {
//            Constants.ANDROID_BUILD_IN_LENS -> {
//                Timber.d("before barcodeScanner isEnabled: ${mBinding?.barcodeScanner?.isEnabled}")
//                Timber.d("before barcodeScanner isActivated: ${mBinding?.barcodeScanner?.isActivated}")
//                if (currentState == VERIFY_SECURITY_CODE_STATE) {
//                    mBinding?.barcodeScanner?.resume()
//                }
//                Timber.d("after barcodeScanner isEnabled: ${mBinding?.barcodeScanner?.isEnabled}")
//                Timber.d("after barcodeScanner isActivated: ${mBinding?.barcodeScanner?.isActivated}")
//            }
//
//            Constants.RTSP_WEB_CAM -> {
//            }
//        }
    }

    override fun onStop() {
        super.onStop()
        Timber.d("onStop()")

        if (sharedViewModel.isSingleModuleMode()) {
            stopRtspClient()
        }

        DeviceUtils.stopFdrOnDestroyTime = System.currentTimeMillis()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy()")

        when (mPreferences.webcamType) {
            Constants.ANDROID_BUILD_IN_LENS -> {
                mBinding?.barcodeScanner?.pauseAndWait()
                mBinding?.barcodeScanner?.visibility = View.GONE
            }

            Constants.RTSP_WEB_CAM -> {
                //mBinding?.rtspImage?.visibility = View.GONE
                stopRtspClient()
            }
        }

        countDownDisposoble?.dispose()
        stopFdr()
    }

    private fun initUI() {
        mBinding?.footerBar?.leftBtnText = getString(R.string.back)
        mBinding?.footerBar?.btnLeft?.let {
            RxView.clicks(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Timber.d("Click foot bar left button")
                    onClickLeftBtn()
                }
        }

        mBinding?.footerBar?.middleTextView?.visibility = View.VISIBLE
        mBinding?.footerBar?.middleText = getString(R.string.qr_scan_hint)

        mBinding?.footerBar?.btnRight?.visibility = View.INVISIBLE
        mBinding?.footerBar?.rightBtnText = getString(R.string.retry)
        mBinding?.footerBar?.btnRight?.let {
            // Retry scan
            RxView.clicks(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Timber.d("Click foot bar right button")
                    onClickRightBtn()
                }
        }

        startDecode()

        if (sharedViewModel.isSingleMode()) {
            sharedViewModel.clockMode = SharedViewModel.MODE_QR_CODE
        }
    }

    private fun startDecode() {
        Timber.d("startDecode()")

        updateStateUI(Constants.REGISTER_STATE_SCAN_CODE)

        if (mPreferences.applicationMode == Constants.REGISTER_MODE) {
            mBinding?.stateLayout?.visibility = View.VISIBLE
        } else {
            mBinding?.stateLayout?.visibility = View.GONE
        }

        when (sharedViewModel.clockModule) {
            SharedViewModel.MODULE_VISITOR -> {
                mBinding?.visitorRegisterForm?.visibility = View.GONE
                mBinding?.visitorRegisterForm?.initUI(mPreferences, registerViewModel)
            }

            else -> {
                mBinding?.employeeRegisterForm?.visibility = View.GONE
                mBinding?.employeeRegisterForm?.initUI(mPreferences, registerViewModel)
            }
        }

        // init barcode scan view
        when (mPreferences.webcamType) {
            Constants.ANDROID_BUILD_IN_LENS -> {
                // Use android build-in lens
                mBinding?.barcodeScanner?.visibility = View.VISIBLE

                /**
                 * For android unit test
                 */
                if (MainActivity.IS_SKIP_SCAN_CODE) {
                    onGetResultQrCode(MainActivity.testSecurityCode)
                } else {
                    val intent = Intent()
                    intent.putExtra(Intents.Scan.CAMERA_ID, 1)
                    val cameraId = intent.extras?.getInt(Intents.Scan.CAMERA_ID)
                    Timber.d("Init scan view with camera id = $cameraId")
                    mBinding?.barcodeScanner?.initializeFromIntent(intent)
                    mBinding?.barcodeScanner?.resume()
                    mBinding?.barcodeScanner?.decodeSingle(mScanCallback)
                    mBinding?.barcodeScanner?.setStatusText("")
                }
            }

            Constants.RTSP_WEB_CAM -> {
                // Init RTSP surface view
                //mBinding?.rtspImage?.visibility = View.VISIBLE
                startRtspClient()
            }
        }

        (activity as MainActivity).setToolbarVisible(false)
    }

    private fun startRtspClient() {
        Timber.d("startRtspClient()")

        mFdrManager.startFdr(isForQrCode = true)
        mBinding?.fdrFrame?.removeAllViews()
        mBinding?.fdrFrame?.visibility = View.VISIBLE
        mBinding?.fdrFrame?.addView(mFdrManager.mFdrCtrl)

        isRtspClientRunning = true

        rtspDecodeThread = thread (start = true) {
            try {
                Thread.sleep(1000L)     // skip all frames of first second

                while(isRtspClientRunning) {
                    Thread.sleep(200L)
                    //Timber.d("Retrieve RTSP image for QRcode decode")

                    mFdrManager.mFdrCtrl?.rtspPreview?.also { rtspPreview ->
                        if (rtspPreview.data != null) {
                            //Timber.d("Preview data size = ${preview.data.size}")
                            //Timber.d("Preview width = ${preview.width}")
                            //Timber.d("Preview height = ${preview.height}")

                            val preview = rtspPreview.clone()

                            val faceBitmap = Bitmap.createBitmap(
                                preview.width, preview.height, Bitmap.Config.ARGB_8888)
                            faceBitmap.setHasAlpha(false)

                            val rgbBuffer = ByteBuffer.wrap(preview.data)
                            rgbBuffer.rewind()

                            faceBitmap.copyPixelsFromBuffer(rgbBuffer)

                            decodeQrCode(faceBitmap)
                        }
                    }
                }
            } catch (e: InterruptedException) {
            }
        }
    }

    private fun stopRtspClient() {
        Timber.d("stopRtspClient()")

        mBinding?.fdrFrame?.removeAllViews()
        mBinding?.fdrFrame?.visibility = View.GONE
        mFdrManager.stopFdr(true)

        isRtspClientRunning = false
        rtspDecodeThread?.join()
        rtspDecodeThread?.interrupt()
    }

    private fun decodeQrCode(bitmap: Bitmap) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        val reader = QRCodeReader()
        val decodeText: String
        try {
            val result = reader.decode(binaryBitmap)
            decodeText = result.text
        } catch (e: Exception) {
            //Timber.d("Decode QR code failed")
            return
        }

        Timber.d("Decode QR code success, result text = $decodeText")

        if (isRtspClientRunning) {
            (activity as MainActivity).startSubscriber()

            SimpleRxTask.onMain {
                stopRtspClient()
                sharedViewModel.clockData.securityCode = decodeText
                onGetResultQrCode(decodeText)
            }
        }
    }

    private fun initViewModelObservers() {
        qrCodeViewModel.initialLoad.observe(this, Observer { networkState ->
            when (networkState?.status) {
                Status.RUNNING -> {

                }

                Status.SUCCESS -> {

                }

                Status.FAILED -> {
                    Timber.d("onError, message: ${networkState.msg}")
                }
            }
        })

        qrCodeViewModel.verifyCodeResult.observe(this, Observer { result ->
            mBinding?.barcodeScanner?.visibility = View.GONE
            //mBinding?.rtspImage?.visibility = View.GONE

            when (result) {
                QrCodeViewModel.VERIFY_SECURITY_SUCCESS -> {
                    // release camera
                    mBinding?.barcodeScanner?.pauseAndWait()
                    mBinding?.barcodeScanner?.visibility = View.GONE

                    // start FDR
                    startFdr()
                }

                QrCodeViewModel.VERIFY_SECURITY_FAILED -> {
                    // show error UI to user
                    mBinding?.footerBar?.btnRight?.visibility = View.VISIBLE
                    mBinding?.scanBackgroundImage?.visibility = View.VISIBLE
                    mBinding?.footerBar?.middleText = getString(R.string.qr_verification_error)

                    (activity as MainActivity).sendFailedClockEvent()
                }

                else -> {}
            }
        })

        mFdrManager.fdrStatusLiveData.observe(this, Observer { status ->
            when (status) {
                FdrManager.STATUS_IDENTIFYING_FACE,
                FdrManager.STATUS_FACE_FORWARD_CAMERA -> {
                    //Timber.d("Observe fdrManager status: STATUS_IDENTIFYING_FACE | STATUS_FACE_FORWARD_CAMERA")
                }

                FdrManager.STATUS_GET_FACE_FAILED -> {
                    Timber.d("Observe fdrManager status: STATUS_GET_FACE_FAILED")
                    stopFdr()

                    // show fail message, related to liveness verification
                    Toast.makeText(context!!, "Get face failed", Toast.LENGTH_SHORT).show()
                }

                FdrManager.STATUS_GET_FACE_SUCCESS -> {
                    Timber.d("Observe fdrManager status: STATUS_GET_FACE_SUCCESS")
                    stopFdr()

                    if (sharedViewModel.isSingleModuleMode()) {
                        mBinding?.footerBar?.btnLeft?.visibility = View.INVISIBLE
                    }
                }
            }
        })

        sharedViewModel.restartSingleModeEvent.observe(this, Observer { mode ->
            if (mode == SharedViewModel.MODE_QR_CODE) {
                doSelfRestart()
            }
        })

        sharedViewModel.verifyFinishEvent.observe(this, Observer {
            updateStateUI(Constants.REGISTER_STATE_COMPLETE)
        })

        sharedViewModel.registerFinishEvent.observe(this, Observer {
            updateStateUI(Constants.REGISTER_STATE_COMPLETE)
        })

        /**
         * Employee and Visitor's watershed
         */
        registerViewModel.errorToastEvent.observe(this, Observer { errorMessage ->
            Toast.makeText(context!!, errorMessage, Toast.LENGTH_LONG).show()
        })

        registerViewModel.registerStateEvent.observe(this, Observer { state ->
            state?.let {
                when (state) {
                    RegisterFormState.EMPTY_SECURITY_CODE -> {
                        Timber.d("RegisterFormState.EMPTY_SECURITY_CODE")
                        DeviceUtils.showRegisterHintDialog(context, getString(R.string.form_security_code_empty_hint))
                    }

                    RegisterFormState.VALID_SECURITY_CODE -> {
                        Timber.d("RegisterFormState.VALID_SECURITY_CODE")

                        updateStateUI(Constants.REGISTER_STATE_FILL_FORM)

                        // continue to fill in the form state
                        mBinding?.barcodeScanner?.visibility = View.GONE
                        mBinding?.scanBackgroundImage?.visibility = View.GONE

                        val securityCode = sharedViewModel.clockData.securityCode ?: ""
                        when (sharedViewModel.clockModule) {
                            SharedViewModel.MODULE_VISITOR -> {
                                mBinding?.visitorRegisterForm?.visibility = View.VISIBLE
                                mBinding?.visitorRegisterForm?.initUI(mPreferences, registerViewModel)
                                mBinding?.visitorRegisterForm?.setSecurityEditTextDisable(
                                    securityCode
                                )
                            }

                            else -> {
                                mBinding?.employeeRegisterForm?.visibility = View.VISIBLE
                                mBinding?.employeeRegisterForm?.initUI(mPreferences, registerViewModel)
                                mBinding?.employeeRegisterForm?.setSecurityEditTextDisable(
                                    securityCode
                                )
                            }
                        }

                        mBinding?.footerBar?.btnRight?.visibility = View.VISIBLE
                        mBinding?.footerBar?.rightBtnText = getString(R.string.done)
                    }

                    RegisterFormState.INVALID_SECURITY_CODE -> {
                        Timber.d("RegisterFormState.INVALID_SECURITY_CODE")

                        updateStateUI(Constants.REGISTER_STATE_FILL_FORM, true)

                        // skip to face enroll state
                        mBinding?.barcodeScanner?.visibility = View.GONE
                        mBinding?.scanBackgroundImage?.visibility = View.GONE
                        mBinding?.footerBar?.btnRight?.visibility = View.INVISIBLE

                        startRetrain()
                    }

                    RegisterFormState.MUST_CHECK_SECURITY_CODE -> {
                        Timber.d("RegisterFormState.MUST_CHECK_SECURITY_CODE")
                        DeviceUtils.showRegisterHintDialog(context, getString(R.string.form_must_check_security_hint))
                    }

                    RegisterFormState.EMPTY_EMAIL -> {
                        Timber.d("RegisterFormState.EMPTY_EMAIL")
                        DeviceUtils.showRegisterHintDialog(context, getString(R.string.form_email_empty_hint))
                    }

                    RegisterFormState.INVALID_EMAIL_FORMAT -> {
                        Timber.d("RegisterFormState.INVALID_EMAIL_FORMAT")
                        DeviceUtils.showRegisterHintDialog(context, getString(R.string.form_invalid_email_format_hint))
                    }


                    // visitor part
                    RegisterFormState.EMPTY_VISITOR_NAME -> {
                        Timber.d("RegisterFormState.EMPTY_VISITOR_NAME")
                        DeviceUtils.showRegisterHintDialog(context, getString(R.string.form_visitor_name_empty_hint))
                    }

                    RegisterFormState.EMPTY_MOBILE_PHONE -> {
                        Timber.d("RegisterFormState.EMPTY_MOBILE_PHONE")
                        DeviceUtils.showRegisterHintDialog(context, getString(R.string.form_mobile_phone_empty_hint))
                    }


                    // employee part
                    RegisterFormState.EMPTY_EMPLOYEE_ID -> {
                        Timber.d("RegisterFormState.EMPTY_EMPLOYEE_ID")
                        DeviceUtils.showRegisterHintDialog(context, getString(R.string.form_employee_id_empty_hint))
                    }

                    RegisterFormState.EMPTY_EMPLOYEE_NAME -> {
                        Timber.d("RegisterFormState.EMPTY_EMPLOYEE_NAME")
                        DeviceUtils.showRegisterHintDialog(context, getString(R.string.form_employee_name_empty_hint))
                    }

                    RegisterFormState.EMPTY_PASSWORD -> {
                        Timber.d("RegisterFormState.EMPTY_PASSWORD")
                        DeviceUtils.showRegisterHintDialog(context, getString(R.string.form_password_empty_hint))
                    }

                    RegisterFormState.INVALID_PASSWORD_FORMAT -> {
                        Timber.d("RegisterFormState.INVALID_PASSWORD_FORMAT")
                        DeviceUtils.showRegisterHintDialog(context, getString(R.string.form_invalid_password_format_hint))
                    }

                    RegisterFormState.EMPLOYEE_EXIST_HINT -> {
                        Timber.d("RegisterFormState.EMPLOYEE_EXIST_HINT")
                        DeviceUtils.showRegisterHintDialog(context, getString(R.string.form_employee_register_exist_hint))
                    }

                    RegisterFormState.VISITOR_EXIST_HINT -> {
                        Timber.d("RegisterFormState.VISITOR_EXIST_HINT")
                        DeviceUtils.showRegisterHintDialog(context, getString(R.string.form_visitor_register_exist_hint))
                    }

                    else -> {}
                }
            }
        })
    }

    private fun startRetrain() {
        mBinding?.exitProfileText1?.visibility = View.VISIBLE
        mBinding?.exitProfileText2?.visibility = View.VISIBLE

        countDownDisposoble = RxCountDownTimer.create(DeviceUtils.EXIST_PROFILE_SKIP_TIME, 1000)
            .subscribe {millisUntilFinished ->

                if (millisUntilFinished == 0L) {
                    mBinding?.exitProfileText1?.visibility = View.GONE
                    mBinding?.exitProfileText2?.visibility = View.GONE

                    // information should get before start FDR
                    startFdr()
                }
            }
    }

    private fun retryQrCodeScan() {
        Timber.d("retryQrCodeScan()")

        updateStateUI(Constants.REGISTER_STATE_SCAN_CODE)

        mBinding?.scanBackgroundImage?.visibility = View.GONE
        mBinding?.footerBar?.btnRight?.visibility = View.INVISIBLE
        mBinding?.footerBar?.middleTextView?.visibility = View.VISIBLE
        mBinding?.footerBar?.middleText = getString(R.string.qr_scan_hint)

        if (MainActivity.IS_SKIP_SCAN_CODE) {
            onGetResultQrCode(MainActivity.testSecurityCode)
        } else {
            when (mPreferences.webcamType) {
                Constants.ANDROID_BUILD_IN_LENS -> {
                    mBinding?.barcodeScanner?.visibility = View.VISIBLE
                    mBinding?.barcodeScanner?.resume()
                    mBinding?.barcodeScanner?.decodeSingle(mScanCallback)
                }

                Constants.RTSP_WEB_CAM -> {
                    //mBinding?.rtspImage?.visibility = View.VISIBLE
                    startRtspClient()
                }
            }

            (activity as MainActivity).setToolbarVisible(false)
        }
    }

    /**
     * Start FDR module
     */
    override fun startFdr() {
        Timber.d("startFdr(), fdrFrame.childCount = ${mBinding?.fdrFrame?.childCount}")
        super.startFdr()

        updateStateUI(Constants.REGISTER_STATE_FACE_GET)

        changeFootBarUI()

        mBinding?.visitorRegisterForm?.visibility = View.GONE
        mBinding?.employeeRegisterForm?.visibility = View.GONE

        mFdrManager.startFdr()
        mBinding?.fdrFrame?.removeAllViews()
        mBinding?.fdrFrame?.visibility = View.VISIBLE
        mBinding?.fdrFrame?.addView(mFdrManager.mFdrCtrl)
    }

    override fun stopFdr() {
        Timber.d("stopFdr()")
        super.stopFdr()
        changeFootBarUI()

        mBinding?.fdrFrame?.removeAllViews()
        mBinding?.fdrFrame?.visibility = View.GONE

        mFdrManager.stopFdr()
    }

    override fun changeFootBarUI() {
        when(currentState) {
            VERIFY_SECURITY_CODE_STATE -> {
                if (mPreferences.applicationMode == Constants.REGISTER_MODE) {
                    //mBinding?.footerBar?.btnRight?.visibility = View.VISIBLE
                    //mBinding?.footerBar?.rightBtnText = getString(R.string.submit)
                    mBinding?.footerBar?.middleText = ""
                }
            }

            VERIFY_FACE_RUNNING_STATE -> {
                mBinding?.footerBar?.btnRight?.visibility = View.INVISIBLE
                mBinding?.footerBar?.middleTextView?.visibility = View.VISIBLE
                mBinding?.footerBar?.middleText = getString(R.string.please_face_forward_the_camera)
            }

            VERIFY_FACE_FINISH_STATE -> {
                if (AppFeatureManager.IS_SUPPORT_RETRAIN_FEATURE) {
                    mBinding?.footerBar?.btnRight?.text = context?.getString(R.string.retrain)
                    mBinding?.footerBar?.btnRight?.visibility = View.VISIBLE
                }

                mBinding?.footerBar?.middleTextView?.visibility = View.GONE
            }
        }
    }

    private fun doSelfRestart() {
        retryQrCodeScan()
        currentState = VERIFY_SECURITY_CODE_STATE
    }

    override fun onClickLeftBtn() {
        Timber.d("Click left button on state: $currentState")

        sharedViewModel.fdrResultVisibilityEvent.postValue(View.GONE)

        if (sharedViewModel.isSingleModuleMode()) {
            stopFdr()
            doSelfRestart()
        } else {
            try {
                when (currentState) {
                    VERIFY_SECURITY_CODE_STATE -> {
                        Navigation.findNavController(this.view!!).popBackStack()
                    }

                    VERIFY_FACE_RUNNING_STATE -> {
                        Navigation.findNavController(this.view!!).popBackStack()
                    }

                    VERIFY_FACE_FINISH_STATE -> {
                        Navigation.findNavController(this.view!!).popBackStack()
                    }
                }
            } catch (e: KotlinNullPointerException) {
                e.printStackTrace()
            }
        }
    }

    override fun onClickRightBtn() {
        Timber.d("Click right button on state: $currentState")

        when(currentState) {
            VERIFY_SECURITY_CODE_STATE -> {
                /**
                 * Employee and Visitor's watershed
                 */
                if (mPreferences.applicationMode == Constants.REGISTER_MODE) {
                    val isValid = when (sharedViewModel.clockModule) {
                        SharedViewModel.MODULE_VISITOR -> mBinding?.visitorRegisterForm?.checkRegistrationForm()

                        else -> mBinding?.employeeRegisterForm?.checkRegistrationForm()
                    } ?: false

                    if (isValid) {
                        hideSoftwareKeyboard()
                        // Start FDR
                        startFdr()
                    }
                } else {
                    retryQrCodeScan()
                }
            }

            VERIFY_FACE_RUNNING_STATE -> {}

            VERIFY_FACE_FINISH_STATE -> {
                if (AppFeatureManager.IS_SUPPORT_RETRAIN_FEATURE) {
                    (activity as MainActivity).showRetrainPage()
                }
            }
        }
    }

    private fun onGetResultQrCode(code: String) {
        sharedViewModel.clockData.securityCode = code

        /**
         * Employee and Visitor's watershed
         */
        if (mPreferences.applicationMode == Constants.REGISTER_MODE) {
            mBinding?.barcodeScanner?.pauseAndWait()
            mBinding?.barcodeScanner?.visibility = View.GONE
            changeFootBarUI()

            registerViewModel.checkSecurityCode(code)
        } else {
            qrCodeViewModel.verify(code, sharedViewModel.clockModule)
        }

        (activity as MainActivity).setToolbarVisible(true)
    }

    private val mScanCallback = object: BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            if (result.text == null) {
                return
            }

//            mBeepManager.playBeepSound()

            Timber.d("mScanCallback result = ${result.text}")

            val code = result.text
            onGetResultQrCode(code)
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
    }

    private fun updateStateUI(state: Int, showUserExistUI: Boolean = false) {
        if (mPreferences.applicationMode == Constants.REGISTER_MODE) {
            mBinding?.registerState = state
        }

        if (sharedViewModel.isSingleModuleMode()) {
            if (state >= Constants.REGISTER_STATE_FILL_FORM && !showUserExistUI) {
                mBinding?.footerBar?.btnLeft?.visibility = View.VISIBLE
            } else {
                mBinding?.footerBar?.btnLeft?.visibility = View.INVISIBLE
            }

            when (state) {
                Constants.REGISTER_STATE_FILL_FORM,
                Constants.REGISTER_STATE_FACE_GET -> {
                    mBinding?.footerBar?.leftBtnText = getString(R.string.cancel)
                }

                Constants.REGISTER_STATE_COMPLETE -> {
                    mBinding?.footerBar?.leftBtnText = getString(R.string.confirm)
                }
            }
        }
    }
}