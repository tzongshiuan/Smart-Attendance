package com.gorilla.attendance.ui.securityCode

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.gorilla.attendance.R
import com.gorilla.attendance.data.model.NetworkState
import com.gorilla.attendance.data.model.RegisterFormState
import com.gorilla.attendance.databinding.SecurityCodeFragmentBinding
import com.gorilla.attendance.di.Injectable
import com.gorilla.attendance.ui.common.BaseFragment
import com.gorilla.attendance.ui.common.FootBarBaseInterface
import com.gorilla.attendance.ui.common.SharedViewModel
import com.gorilla.attendance.ui.main.MainActivity
import com.gorilla.attendance.utils.AppFeatureManager
import com.gorilla.attendance.utils.Constants
import com.gorilla.attendance.utils.DeviceUtils
import com.gorilla.attendance.utils.FdrManager
import com.gorilla.attendance.utils.rxCountDownTimer.RxCountDownTimer
import com.jakewharton.rxbinding.view.RxView
import io.reactivex.disposables.Disposable
import rx.android.schedulers.AndroidSchedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SecurityCodeFragment: BaseFragment(), FootBarBaseInterface, Injectable {
    private var mBinding: SecurityCodeFragmentBinding? = null

    private lateinit var securityCodeViewModel: SecurityCodeViewModel

    private var isCheckedCode = false

    private var isRetrain = false

    private var countDownDisposoble: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate()")

        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = SecurityCodeFragmentBinding.inflate(inflater, container, false)

        return mBinding?.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        securityCodeViewModel =
            ViewModelProviders.of(this, factory).get(SecurityCodeViewModel::class.java)

        mBinding?.viewModel = securityCodeViewModel

        initUI()

        initViewModelObservers()
    }

    override fun onStart() {
        super.onStart()
        Timber.d("onStart()")
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume()")

        mBinding?.fdrFrame?.foreground = null

        changeTitle()
    }

    override fun onStop() {
        super.onStop()
        Timber.d("onStop()")

        mBinding?.fdrFrame?.foreground = ColorDrawable(Color.BLACK)

        DeviceUtils.stopFdrOnDestroyTime = System.currentTimeMillis()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy()")

        countDownDisposoble?.dispose()
        stopFdr()

        isStartRegister = false
    }

    private fun changeTitle() {
        if (mPreferences.applicationMode == Constants.VERIFICATION_MODE) {
            mBinding?.securityCodeEditText?.hint = getString(R.string.security_hint)
            sharedViewModel.changeTitleEvent.postValue(getString(R.string.security_verification_title))
        } else {
            mBinding?.securityCodeEditText?.hint = getString(R.string.security_hint_register)
            when (sharedViewModel.clockModule) {
                SharedViewModel.MODULE_VISITOR -> sharedViewModel.changeTitleEvent.postValue(
                    getString(R.string.visitor_registration_form)
                )
                else -> sharedViewModel.changeTitleEvent.postValue(getString(R.string.employee_registration_form))
            }

            isStartRegister = true
        }
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

        mBinding?.securityCodeEditText?.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
            ) {
                mBinding?.footerBar?.btnRight?.performClick()
            }

            false
        }

        mBinding?.settingTrigger?.setOnClickListener {
            (activity as MainActivity).softClickTimeText()
        }

        startDecode()

        if (sharedViewModel.isSingleMode()) {
            sharedViewModel.clockMode = SharedViewModel.MODE_SECURITY
        }
    }

    private fun startDecode(isUserAgree: Boolean = false) {
        if (!isUserAgree && sharedViewModel.isNeedUserAgreement()) {
            sharedViewModel.userAgreeEvent.postValue(true)
            return
        }

        updateStateUI(Constants.REGISTER_STATE_SCAN_CODE)

        /**
         * Employee and Visitor's watershed
         */
        if (mPreferences.applicationMode == Constants.REGISTER_MODE) {
            mBinding?.footerBar?.rightBtnText = getString(R.string.verification)
            mBinding?.stateLayout?.visibility = View.VISIBLE
            mBinding?.securityGroup?.visibility = View.VISIBLE

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
        } else {
            mBinding?.stateLayout?.visibility = View.GONE
            mBinding?.securityGroup?.visibility = View.VISIBLE
            mBinding?.footerBar?.rightBtnText = getString(R.string.verification)
        }
    }

    private fun initViewModelObservers() {
        securityCodeViewModel.initialLoad.observe(this, Observer { state ->
            when (state) {
                NetworkState.LOADING -> {}
                NetworkState.LOADED -> {}
                is NetworkState.error -> {}
            }
        })

        securityCodeViewModel.verifyCodeResult.observe(this, Observer { result ->
            when (result) {
                SecurityCodeViewModel.VERIFY_SECURITY_SUCCESS -> {
                    mBinding?.securityCodeEditText?.setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            context!!,
                            R.drawable.rounded_border
                        )
                    )
                    mBinding?.errorHintText?.visibility = View.GONE

                    // start FDR
                    startFdr()
                }

                SecurityCodeViewModel.VERIFY_SECURITY_FAILED -> {
                    // show error UI to user
                    mBinding?.securityCodeEditText?.setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            context!!,
                            R.drawable.rounded_border_error
                        )
                    )
                    mBinding?.errorHintText?.visibility = View.VISIBLE

                    (activity as MainActivity).sendFailedClockEvent()
                }

                SecurityCodeViewModel.SECURITY_EMPTY -> {
                    Toast.makeText(context!!, R.string.empty_security_code_msg, Toast.LENGTH_SHORT)
                        .show()
                }

                else -> {
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
            if (mode == SharedViewModel.MODE_SECURITY) {
                doSelfRestart()
            }
        })

        sharedViewModel.userHadAgreeEvent.observe(this, Observer {
            startDecode(true)
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
                        DeviceUtils.showRegisterHintDialog(
                            context,
                            getString(R.string.form_security_code_empty_hint)
                        )
                    }

                    RegisterFormState.VALID_SECURITY_CODE -> {
                        Timber.d("RegisterFormState.VALID_SECURITY_CODE")

                        updateStateUI(Constants.REGISTER_STATE_FILL_FORM)

                        // continue to fill in the form state
                        mBinding?.securityGroup?.visibility = View.GONE
                        val securityCode = mBinding?.securityCodeEditText?.text.toString()

                        when (sharedViewModel.clockModule) {
                            SharedViewModel.MODULE_VISITOR -> {
                                mBinding?.visitorRegisterForm?.visibility = View.VISIBLE
                                mBinding?.visitorRegisterForm?.setSecurityEditTextDisable(
                                    securityCode
                                )
                            }

                            else -> {
                                mBinding?.employeeRegisterForm?.visibility = View.VISIBLE
                                mBinding?.employeeRegisterForm?.setSecurityEditTextDisable(
                                    securityCode
                                )
                            }
                        }

                        mBinding?.footerBar?.btnRight?.visibility = View.VISIBLE
                        mBinding?.footerBar?.rightBtnText = getString(R.string.done)
                        isCheckedCode = true
                    }

                    RegisterFormState.INVALID_SECURITY_CODE -> {
                        Timber.d("RegisterFormState.INVALID_SECURITY_CODE")

                        updateStateUI(Constants.REGISTER_STATE_FILL_FORM, true)

                        // skip to face enroll state
                        mBinding?.securityGroup?.visibility = View.GONE
                        mBinding?.footerBar?.btnRight?.visibility = View.INVISIBLE

                        startRetrain()
                    }

                    RegisterFormState.MUST_CHECK_SECURITY_CODE -> {
                        Timber.d("RegisterFormState.MUST_CHECK_SECURITY_CODE")
                        DeviceUtils.showRegisterHintDialog(
                            context,
                            getString(R.string.form_must_check_security_hint)
                        )
                    }

                    RegisterFormState.EMPTY_EMAIL -> {
                        Timber.d("RegisterFormState.EMPTY_EMAIL")
                        DeviceUtils.showRegisterHintDialog(
                            context,
                            getString(R.string.form_email_empty_hint)
                        )
                    }

                    RegisterFormState.INVALID_EMAIL_FORMAT -> {
                        Timber.d("RegisterFormState.INVALID_EMAIL_FORMAT")
                        DeviceUtils.showRegisterHintDialog(
                            context,
                            getString(R.string.form_invalid_email_format_hint)
                        )
                    }

                    // visitor part
                    RegisterFormState.EMPTY_VISITOR_NAME -> {
                        Timber.d("RegisterFormState.EMPTY_VISITOR_NAME")
                        DeviceUtils.showRegisterHintDialog(
                            context,
                            getString(R.string.form_visitor_name_empty_hint)
                        )
                    }

                    RegisterFormState.EMPTY_MOBILE_PHONE -> {
                        Timber.d("RegisterFormState.EMPTY_MOBILE_PHONE")
                        DeviceUtils.showRegisterHintDialog(
                            context,
                            getString(R.string.form_mobile_phone_empty_hint)
                        )
                    }


                    // employee part
                    RegisterFormState.EMPTY_EMPLOYEE_ID -> {
                        Timber.d("RegisterFormState.EMPTY_EMPLOYEE_ID")
                        DeviceUtils.showRegisterHintDialog(
                            context,
                            getString(R.string.form_employee_id_empty_hint)
                        )
                    }

                    RegisterFormState.EMPTY_EMPLOYEE_NAME -> {
                        Timber.d("RegisterFormState.EMPTY_EMPLOYEE_NAME")
                        DeviceUtils.showRegisterHintDialog(
                            context,
                            getString(R.string.form_employee_name_empty_hint)
                        )
                    }

                    RegisterFormState.EMPTY_PASSWORD -> {
                        Timber.d("RegisterFormState.EMPTY_PASSWORD")
                        DeviceUtils.showRegisterHintDialog(
                            context,
                            getString(R.string.form_password_empty_hint)
                        )
                    }

                    RegisterFormState.INVALID_PASSWORD_FORMAT -> {
                        Timber.d("RegisterFormState.INVALID_PASSWORD_FORMAT")
                        DeviceUtils.showRegisterHintDialog(
                            context,
                            getString(R.string.form_invalid_password_format_hint)
                        )
                    }

                    RegisterFormState.EMPLOYEE_EXIST_HINT -> {
                        Timber.d("RegisterFormState.EMPLOYEE_EXIST_HINT")
                        DeviceUtils.showRegisterHintDialog(
                            context,
                            getString(R.string.form_employee_register_exist_hint)
                        )
                    }

                    RegisterFormState.VISITOR_EXIST_HINT -> {
                        Timber.d("RegisterFormState.VISITOR_EXIST_HINT")
                        DeviceUtils.showRegisterHintDialog(
                            context,
                            getString(R.string.form_visitor_register_exist_hint)
                        )
                    }

                    else -> {
                    }
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

    /**
     * Start FDR module
     */
    override fun startFdr() {
        Timber.d("startFdr(), fdrFrame.childCount = ${mBinding?.fdrFrame?.childCount}")
        super.startFdr()

        updateStateUI(Constants.REGISTER_STATE_FACE_GET)

        changeFootBarUI()

        mBinding?.securityGroup?.visibility = View.GONE
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

        mBinding?.fdrFrame?.visibility = View.GONE
        mBinding?.fdrFrame?.removeAllViews()

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
        startDecode()
        mBinding?.securityCodeEditText?.text?.clear()
        mBinding?.footerBar?.btnRight?.visibility = View.VISIBLE
        currentState = VERIFY_SECURITY_CODE_STATE

        isCheckedCode = false

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
                    isCheckedCode = false
                    mBinding?.securityGroup?.visibility = View.VISIBLE
                    mBinding?.securityCodeEditText?.text?.clear()
                    mBinding?.employeeRegisterForm?.visibility = View.GONE
                    mBinding?.visitorRegisterForm?.visibility = View.GONE
                    mBinding?.footerBar?.btnRight?.visibility = View.VISIBLE
                    mBinding?.footerBar?.rightBtnText = getString(R.string.submit)
                    updateStateUI(Constants.REGISTER_STATE_SCAN_CODE)
                }

                Constants.REGISTER_STATE_FACE_GET -> {
                    stopFdr()
                    currentState = VERIFY_SECURITY_CODE_STATE
                    if (isRetrain) {
                        isCheckedCode = false
                        // back to scan code stage
                        mBinding?.securityGroup?.visibility = View.VISIBLE
                        mBinding?.securityCodeEditText?.text?.clear()
                        mBinding?.footerBar?.btnRight?.visibility = View.VISIBLE
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
                stopFdr()
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
                    if (!isCheckedCode) {
                        /**
                         * Not checked security code before
                         */
                        val securityCode = mBinding?.securityCodeEditText?.text.toString()
                        if (securityCode.isEmpty()) {
                            return
                        }

                        hideSoftwareKeyboard()
                        registerViewModel.checkSecurityCode(securityCode)
                    } else {
                        val isValid = when (sharedViewModel.clockModule) {
                            SharedViewModel.MODULE_VISITOR -> mBinding?.visitorRegisterForm?.checkRegistrationForm()

                            else -> mBinding?.employeeRegisterForm?.checkRegistrationForm()
                        } ?: false

                        if (isValid) {
                            hideSoftwareKeyboard()
                            // Start FDR
                            startFdr()
                        }
                    }
                } else {
                    val code = mBinding?.securityCodeEditText?.text.toString()
                    sharedViewModel.clockData.securityCode = code
                    securityCodeViewModel.verify(code, sharedViewModel.clockModule)
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