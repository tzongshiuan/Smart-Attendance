package com.gorilla.attendance.ui.rfid

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.gorilla.attendance.R
import com.gorilla.attendance.data.model.NetworkState
import com.gorilla.attendance.data.model.RegisterFormState
import com.gorilla.attendance.databinding.RfidFragmentBinding
import com.gorilla.attendance.di.Injectable
import com.gorilla.attendance.ui.common.BaseFragment
import com.gorilla.attendance.ui.common.FootBarBaseInterface
import com.gorilla.attendance.ui.common.SharedViewModel
import com.gorilla.attendance.ui.main.MainActivity
import com.gorilla.attendance.utils.*
import com.gorilla.attendance.utils.rxCountDownTimer.RxCountDownTimer
import com.jakewharton.rxbinding.view.RxView
import io.reactivex.disposables.Disposable
import rx.android.schedulers.AndroidSchedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class RFIDFragment: BaseFragment(), FootBarBaseInterface, Injectable {
    private var mBinding: RfidFragmentBinding? = null

    @Inject
    lateinit var mNfcManager: NfcManager

    private lateinit var rfidViewModel: RFIDViewModel

    private var countDownDisposoble: Disposable? = null

    private var isRetrain = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate()")

        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = RfidFragmentBinding.inflate(inflater, container, false)

        return mBinding?.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        rfidViewModel = ViewModelProviders.of(this, factory).get(RFIDViewModel::class.java)

        mBinding?.viewModel = rfidViewModel

        initUI()

        initViewModelObservers()

        if (!MainActivity.IS_SKIP_SCAN_CODE) {
            mNfcManager.initNfcManager(context!!)
        }

        if (!MainActivity.IS_SKIP_SCAN_CODE) {
            mNfcManager.start()
            mNfcManager.setEnableReadCard(true)
        }
    }

    override fun onStart() {
        super.onStart()
        Timber.d("onStart()")
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume()")

        mBinding?.fdrFrame?.foreground = null

        mNfcManager.setRfidActive(true)

        if (mPreferences.applicationMode == Constants.VERIFICATION_MODE) {
            sharedViewModel.changeTitleEvent.postValue(getString(R.string.rfid_verification_title))
        } else {
            when (sharedViewModel.clockModule) {
                SharedViewModel.MODULE_VISITOR -> sharedViewModel.changeTitleEvent.postValue(getString(R.string.visitor_registration_form))
                else -> sharedViewModel.changeTitleEvent.postValue(getString(R.string.employee_registration_form))
            }

            isStartRegister = true
        }

        mNfcManager.setEnableReadCard(true)
    }

    override fun onStop() {
        super.onStop()
        Timber.d("onStop()")

        mBinding?.fdrFrame?.foreground = ColorDrawable(Color.BLACK)

        mNfcManager.setRfidActive(false)

        DeviceUtils.stopFdrOnDestroyTime = System.currentTimeMillis()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy()")

        if (!MainActivity.IS_SKIP_SCAN_CODE) {
            mNfcManager.stop()
        }

        countDownDisposoble?.dispose()
        stopFdr()

        isStartRegister = false
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

        mBinding?.footerBar?.btnRight?.let {
            RxView.clicks(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Timber.i("Click foot bar right button")
                    onClickRightBtn()
                }
        }

        mBinding?.settingTrigger?.setOnClickListener {
            (activity as MainActivity).softClickTimeText()
        }

        startScan()
        when (sharedViewModel.clockModule) {
            SharedViewModel.MODULE_VISITOR -> {
                mBinding?.visitorRegisterForm?.initUI(mPreferences, registerViewModel)
            }

            else -> {
                mBinding?.employeeRegisterForm?.initUI(mPreferences, registerViewModel)
            }
        }

        /**
         * For android unit test
         */
        if (MainActivity.IS_SKIP_SCAN_CODE) {
            onReceiveNfcData(MainActivity.testSecurityCode)
        }

        if (sharedViewModel.isSingleMode()) {
            sharedViewModel.clockMode = SharedViewModel.MODE_RFID
        }
    }

    private fun startScan() {
        updateStateUI(Constants.REGISTER_STATE_SCAN_CODE)

        mBinding?.rfidGroup?.visibility = View.VISIBLE
        mBinding?.errorHintText?.visibility = View.GONE

        when (sharedViewModel.clockModule) {
            SharedViewModel.MODULE_VISITOR -> {
                mBinding?.visitorRegisterForm?.visibility = View.GONE
            }

            else -> {
                mBinding?.employeeRegisterForm?.visibility = View.GONE
            }
        }

        /**
         * Employee and Visitor's watershed
         */
        if (mPreferences.applicationMode == Constants.REGISTER_MODE) {
            mBinding?.stateLayout?.visibility = View.VISIBLE
            mBinding?.footerBar?.btnRight?.visibility = View.INVISIBLE
            mBinding?.footerBar?.rightBtnText = getString(R.string.submit)
        } else {
            mBinding?.stateLayout?.visibility = View.GONE
            mBinding?.footerBar?.btnRight?.visibility = View.INVISIBLE
            mBinding?.footerBar?.rightBtnText = getString(R.string.verification)
        }
    }

    private fun initViewModelObservers() {
        mNfcManager.nfcReadStateEvent.observe(this, Observer { state ->
            when (state) {
                NfcManager.NFC_START_INIT_READER -> {
                    Timber.d("NFC state: NFC_START_INIT_READE")
                }

                NfcManager.NFC_START_CARD_READ -> {
                    Timber.d("NFC state: NFC_START_CARD_READ")
                    mNfcManager.setEnableReadCard(false)
                }

                NfcManager.NFC_END_INIT_READER -> {
                    Timber.d("NFC state: NFC_END_INIT_READER")
                    Toast.makeText(context!!, "Usb device detached", Toast.LENGTH_SHORT).show()
                }

                NfcManager.NFC_END_CARD_READ -> {
                    Timber.d("NFC state: NFC_END_CARD_READ")
                }

                NfcManager.NFC_PERMISSION_DENIED,
                NfcManager.NFC_DEVICE_NOT_FOUND,
                NfcManager.NFC_READER_NOT_SUPPORT -> {
                    Timber.d("NFC state: NFC_PERMISSION_DENIED | NFC_DEVICE_NOT_FOUND | NFC_READER_NOT_SUPPORT")
                    //if (!sharedViewModel.isSingleModuleMode()) {
                        //Navigation.findNavController(this.view!!).popBackStack()
                    //}
                }
            }
        })

        mNfcManager.nfcReadData.observe(this, Observer { data ->
            Timber.d("NFC read data = $data")

            onReceiveNfcData(data)
        })

        rfidViewModel.initialLoad.observe(this, Observer { state ->
            when (state) {
                NetworkState.LOADING -> {}
                NetworkState.LOADED -> {}
                is NetworkState.error -> {}
            }
        })

        rfidViewModel.verifyCodeResult.observe(this, Observer { result ->
            when (result) {
                RFIDViewModel.VERIFY_RFID_SUCCESS -> {
                    mBinding?.rfidGroup?.visibility = View.GONE
                    mBinding?.errorHintText?.visibility = View.GONE
                    // start FDR
                    startFdr()
                }

                RFIDViewModel.VERIFY_RFID_FAILED -> {
                    // show error UI to user
                    mBinding?.rfidGroup?.visibility = View.GONE
                    mBinding?.errorHintText?.visibility = View.VISIBLE
                    mNfcManager.setEnableReadCard(true)

                    (activity as MainActivity).sendFailedClockEvent()
                }

                RFIDViewModel.RFID_NULL -> {
                    Toast.makeText(context!!, resources.getString(R.string.rfid_toast_no_result), Toast.LENGTH_SHORT).show()
                    mNfcManager.setEnableReadCard(true)
                }
            }
        })

        mFdrManager.fdrStatusLiveData.observe(this, Observer { status ->
            when (status) {
                FdrManager.STATUS_IDENTIFYING_FACE,
                FdrManager.STATUS_FACE_FORWARD_CAMERA -> {
                    //Timber.d("Observe fdrManager status: STATUS_IDENTIFYING_FACE | STATUS_FACE_FORWARD_CAMERA")
                }

                FdrManager.STATUS_GET_FACE_FAILED -> {}

                FdrManager.STATUS_GET_FACE_SUCCESS -> {
                    Timber.d("Observe fdrManager status: STATUS_GET_FACE_SUCCESS")

                    if (mPreferences.applicationMode == Constants.REGISTER_MODE) {
                        mBinding?.isStateLayoutDarkness = false
                        stopFdr()
                        mBinding?.footerBar?.btnLeft?.visibility = View.INVISIBLE
                    }
                }

                FdrManager.STATUS_GET_FACE_OCCUR -> {}
            }
        })

        sharedViewModel.bottomFaceResultEvent.observe(this, Observer { faceResult ->
            if (faceResult.isSuccess) {
                if (sharedViewModel.isOptionClockMode()) {
                    stopFdr()
                } else {
                    mBinding?.footerBar?.middleTextView?.visibility = View.GONE
                    mBinding?.footerBar?.successTextView?.visibility = View.VISIBLE
                    mBinding?.footerBar?.successText = faceResult.result
                }
            } else {
                currentState = VERIFY_FACE_FINISH_STATE
                mBinding?.footerBar?.middleTextView?.visibility = View.GONE
                mBinding?.footerBar?.failTextView?.visibility = View.VISIBLE
                mBinding?.footerBar?.failText = faceResult.result
            }
        })

        sharedViewModel.restartSingleModeEvent.observe(this, Observer { mode ->
            if (mode == SharedViewModel.MODE_RFID) {
                doSelfRestart()
            }
        })

        sharedViewModel.verifyFinishEvent.observe(this, Observer {
            currentState = VERIFY_FACE_FINISH_STATE
            updateStateUI(Constants.REGISTER_STATE_COMPLETE)
        })

        sharedViewModel.registerFinishEvent.observe(this, Observer {
            updateStateUI(Constants.REGISTER_STATE_COMPLETE)

            if (sharedViewModel.isSingleModuleMode()) {
                when (sharedViewModel.clockModule) {
                    SharedViewModel.MODULE_VISITOR -> {
                        mBinding?.visitorRegisterForm?.clearUI()
                    }

                    else -> {
                        mBinding?.employeeRegisterForm?.clearUI()
                    }
                }
            }
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

                        when (sharedViewModel.clockModule) {
                            SharedViewModel.MODULE_VISITOR -> {
                                mBinding?.visitorRegisterForm?.visibility = View.VISIBLE
                            }

                            else -> {
                                mBinding?.employeeRegisterForm?.visibility = View.VISIBLE
                            }
                        }

                        mBinding?.footerBar?.btnRight?.visibility = View.VISIBLE
                        mBinding?.footerBar?.rightBtnText = getString(R.string.done)
                    }

                    RegisterFormState.INVALID_SECURITY_CODE -> {
                        Timber.d("RegisterFormState.INVALID_SECURITY_CODE")

                        updateStateUI(Constants.REGISTER_STATE_FILL_FORM, true)

                        // skip to face enroll state
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
        isRetrain = true

        mBinding?.exitProfileText1?.visibility = View.VISIBLE
        mBinding?.exitProfileText2?.visibility = View.VISIBLE
        mBinding?.footerBar?.btnLeft?.visibility = View.INVISIBLE

        countDownDisposoble = RxCountDownTimer.create(DeviceUtils.EXIST_PROFILE_SKIP_TIME, 1000)
            .subscribe {millisUntilFinished ->

                if (millisUntilFinished == 0L) {
                    mBinding?.exitProfileText1?.visibility = View.GONE
                    mBinding?.exitProfileText2?.visibility = View.GONE
                    mBinding?.footerBar?.btnLeft?.visibility = View.VISIBLE

                    // information should get before start FDR
                    startFdr()
                }
            }
    }

    private fun onReceiveNfcData(data: String?) {
        Timber.d("onReceiveNfcData(), RFID: $data")

        if (data == null) {
            mNfcManager.setEnableReadCard(true)
            Timber.e("Read null NFC data !!")
            return
        }

        /**
         * Employee and Visitor's watershed
         */
        if (mPreferences.applicationMode == Constants.REGISTER_MODE) {
            updateStateUI(Constants.REGISTER_STATE_SCAN_CODE)
            changeFootBarUI()
            mBinding?.rfidGroup?.visibility = View.GONE
            mBinding?.errorHintText?.visibility = View.GONE

            registerViewModel.checkRfid(data)
            when (sharedViewModel.clockModule) {
                SharedViewModel.MODULE_VISITOR ->  {
                    //mBinding?.visitorRegisterForm?.rfid = data
                }

                else -> {
                    data.let {
                        mBinding?.employeeRegisterForm?.rfid = it
                    }
                }
            }
        } else {
            sharedViewModel.clockData.rfid = data
            rfidViewModel.verify(data, sharedViewModel.clockModule)
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
            VERIFY_SECURITY_CODE_STATE -> {}

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
                mBinding?.footerBar?.successTextView?.visibility = View.GONE
                mBinding?.footerBar?.failTextView?.visibility = View.GONE
            }
        }
    }

    private fun doSelfRestart() {
        stopFdr()
        startScan()
        mNfcManager.setEnableReadCard(true)
        currentState = VERIFY_SECURITY_CODE_STATE

        mBinding?.footerBar?.successTextView?.visibility = View.GONE
        mBinding?.footerBar?.failTextView?.visibility = View.GONE
    }

    override fun onClickLeftBtn() {
        Timber.d("Click left button on state: $currentState")

        sharedViewModel.fdrResultVisibilityEvent.postValue(View.GONE)

        if (mPreferences.applicationMode == Constants.REGISTER_MODE) {
            when (mBinding?.registerState) {
                Constants.REGISTER_STATE_SCAN_CODE -> {
                    (activity as MainActivity).navBack()
                }

                Constants.REGISTER_STATE_FILL_FORM -> {
                    doSelfRestart()
                    mBinding?.employeeRegisterForm?.visibility = View.GONE
                    mBinding?.visitorRegisterForm?.visibility = View.GONE
                    mBinding?.footerBar?.btnRight?.visibility = View.INVISIBLE
                    mBinding?.footerBar?.rightBtnText = getString(R.string.submit)
                    updateStateUI(Constants.REGISTER_STATE_SCAN_CODE)
                }

                Constants.REGISTER_STATE_FACE_GET -> {
                    stopFdr()
                    currentState = VERIFY_SECURITY_CODE_STATE
                    if (isRetrain) {
                        // back to scan code stage
                        doSelfRestart()
                        mBinding?.footerBar?.btnRight?.visibility = View.INVISIBLE
                        mBinding?.footerBar?.rightBtnText = getString(R.string.submit)
                        updateStateUI(Constants.REGISTER_STATE_SCAN_CODE)
                    } else {
                        // back to enter register info stage
                        when (sharedViewModel.clockModule) {
                            SharedViewModel.MODULE_VISITOR -> {
                                mBinding?.visitorRegisterForm?.visibility = View.VISIBLE
                            }

                            else -> {
                                mBinding?.employeeRegisterForm?.visibility = View.VISIBLE
                            }
                        }
                        mBinding?.footerBar?.btnRight?.visibility = View.VISIBLE
                        updateStateUI(Constants.REGISTER_STATE_FILL_FORM)
                    }
                }

                Constants.REGISTER_STATE_COMPLETE -> {
                    if (sharedViewModel.isSingleModuleMode()) {
                        doSelfRestart()
                    } else {
                        (activity as MainActivity).backToPreviousPage()
                    }
                }
            }
            isRetrain = false
        } else {
            if (sharedViewModel.isSingleModuleMode()) {
                doSelfRestart()
            } else {
                try {
                    when (currentState) {
                        VERIFY_SECURITY_CODE_STATE -> {
                            (activity as MainActivity).navBack()
                        }

                        VERIFY_FACE_RUNNING_STATE -> {
                            (activity as MainActivity).navBack()
                        }

                        VERIFY_FACE_FINISH_STATE -> {
                            (activity as MainActivity).backToPreviousPage()
                        }
                    }
                } catch (e: KotlinNullPointerException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onClickRightBtn() {
        Timber.i("Click right button on state: $currentState")

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
                    // do nothing
                }
            }

            VERIFY_FACE_RUNNING_STATE -> {}

            VERIFY_FACE_FINISH_STATE -> {
                // protected machanism
                currentState = VERIFY_SECURITY_CODE_STATE

                if (AppFeatureManager.IS_SUPPORT_RETRAIN_FEATURE) {
                    (activity as MainActivity).showRetrainPage()
                }
            }
        }
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
        }

        when (state) {
            Constants.REGISTER_STATE_SCAN_CODE -> {
                mBinding?.isStateLayoutDarkness = false
                mBinding?.footerBar?.leftBtnText = getString(R.string.back)
            }

            Constants.REGISTER_STATE_FILL_FORM -> {
                mBinding?.isStateLayoutDarkness = false
                mBinding?.footerBar?.leftBtnText = getString(R.string.back)
            }

            Constants.REGISTER_STATE_FACE_GET -> {
                mBinding?.isStateLayoutDarkness = true
                mBinding?.footerBar?.leftBtnText = getString(R.string.back)
            }

            Constants.REGISTER_STATE_COMPLETE -> {
                mBinding?.isStateLayoutDarkness = false
                //mBinding?.footerBar?.btnLeft?.visibility = View.INVISIBLE
                //mBinding?.footerBar?.leftBtnText = getString(R.string.confirm)
            }
        }
    }
}