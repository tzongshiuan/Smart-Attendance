package com.gorilla.attendance.ui.securityCode

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
import com.gorilla.attendance.data.model.RegisterFormState
import com.gorilla.attendance.data.model.Status
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

        if (mPreferences.applicationMode == Constants.VERIFICATION_MODE) {
            sharedViewModel.changeTitleEvent.postValue(getString(R.string.security_verification_title))
        } else {
            when (sharedViewModel.clockModule) {
                SharedViewModel.MODULE_VISITOR -> sharedViewModel.changeTitleEvent.postValue(
                    getString(R.string.visitor_registration_form)
                )
                else -> sharedViewModel.changeTitleEvent.postValue(getString(R.string.employee_registration_form))
            }
        }
    }

    override fun onStop() {
        super.onStop()
        Timber.d("onStop()")

        DeviceUtils.stopFdrOnDestroyTime = System.currentTimeMillis()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy()")

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

        mBinding?.footerBar?.btnRight?.let {
            RxView.clicks(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Timber.d("Click foot bar right button")
                    onClickRightBtn()
                }
        }

        mBinding?.securityCodeEditText?.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
            ) {
                mBinding?.footerBar?.btnRight?.performClick()
            }

            false
        }

        startDecode()

        if (sharedViewModel.isSingleMode()) {
            sharedViewModel.clockMode = SharedViewModel.MODE_SECURITY
        }
    }

    private fun startDecode() {
        updateStateUI(Constants.REGISTER_STATE_SCAN_CODE)

        /**
         * Employee and Visitor's watershed
         */
        if (mPreferences.applicationMode == Constants.REGISTER_MODE) {
            mBinding?.footerBar?.rightBtnText = getString(R.string.submit)
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
        securityCodeViewModel.initialLoad.observe(this, Observer { networkState ->
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

                FdrManager.STATUS_GET_FACE_FAILED -> {
                    Timber.d("Observe fdrManager status: STATUS_GET_FACE_FAILED")
                    stopFdr()

                    // show fail message, related to liveness verification
                    Toast.makeText(context!!, "Get face failed", Toast.LENGTH_SHORT).show()
                }

                FdrManager.STATUS_GET_FACE_SUCCESS -> {
                    Timber.d("Observe fdrManager status: STATUS_GET_FACE_SUCCESS")
                    stopFdr()
                }

                FdrManager.STATUS_GET_FACE_TIMEOUT -> {
                    Timber.d("Observe fdrManager status: STATUS_GET_FACE_TIMEOUT")
                    stopFdr()

                    if (sharedViewModel.isSingleModuleMode()) {
                        mBinding?.footerBar?.btnLeft?.visibility = View.INVISIBLE
                    }
                }
            }
        })

        sharedViewModel.restartSingleModeEvent.observe(this, Observer { mode ->
            if (mode == SharedViewModel.MODE_SECURITY) {
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
            }
        }
    }

    private fun doSelfRestart() {
        startDecode()
        mBinding?.securityCodeEditText?.text?.clear()
        mBinding?.footerBar?.btnRight?.visibility = View.VISIBLE
        currentState = VERIFY_SECURITY_CODE_STATE

        isCheckedCode = false
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
                    if (!isCheckedCode) {
                        /**
                         * Not checked security code before
                         */
                        val securityCode = mBinding?.securityCodeEditText?.text.toString()
                        if (securityCode.isEmpty()) {
                            return
                        }

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