package com.gorilla.attendance.ui.faceIdentification

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.gorilla.attendance.R
import com.gorilla.attendance.data.model.NetworkState
import com.gorilla.attendance.data.model.RegisterFormState
import com.gorilla.attendance.databinding.FaceIdentificationFragmentBinding
import com.gorilla.attendance.di.Injectable
import com.gorilla.attendance.ui.common.BaseFragment
import com.gorilla.attendance.ui.common.FootBarBaseInterface
import com.gorilla.attendance.ui.common.SharedViewModel
import com.gorilla.attendance.ui.main.MainActivity
import com.gorilla.attendance.utils.*
import com.jakewharton.rxbinding.view.RxView
import rx.android.schedulers.AndroidSchedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit

class FaceIdentificationFragment: BaseFragment(), FootBarBaseInterface, Injectable {
    private var mBinding: FaceIdentificationFragmentBinding? = null

    private lateinit var faceIdentificationViewModel: FaceIdentificationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate()")

        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = FaceIdentificationFragmentBinding.inflate(inflater, container, false)

        return mBinding?.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        faceIdentificationViewModel = ViewModelProviders.of(this, factory).get(FaceIdentificationViewModel::class.java)

        mBinding?.viewModel = faceIdentificationViewModel

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
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume()")

        mBinding?.fdrFrame?.foreground = null

        if (mPreferences.applicationMode == Constants.VERIFICATION_MODE) {
            sharedViewModel.changeTitleEvent.postValue(getString(R.string.face_identification_title))
        } else {
            when (sharedViewModel.clockModule) {
                SharedViewModel.MODULE_VISITOR -> sharedViewModel.changeTitleEvent.postValue(getString(R.string.visitor_registration_form))
                else -> sharedViewModel.changeTitleEvent.postValue(getString(R.string.employee_registration_form))
            }

            isStartRegister = true
        }
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

        mBinding?.footerBar?.middleTextView?.visibility = View.VISIBLE

        mBinding?.footerBar?.btnRight?.let {
            // Retry scan
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

        startScanFace()

        if (sharedViewModel.isSingleMode()) {
            sharedViewModel.clockMode = SharedViewModel.MODE_FACE_IDENTIFICATION
        }
    }

    private fun startScanFace() {
        /**
         * Employee and Visitor's watershed
         */
        if (mPreferences.applicationMode == Constants.REGISTER_MODE) {
            // Not support now
            Toast.makeText(context!!, "No matching mode for use now, please check server setting."
                , Toast.LENGTH_LONG).show()

            mBinding?.footerBar?.btnLeft?.visibility = View.INVISIBLE
            mBinding?.footerBar?.btnRight?.visibility = View.INVISIBLE
        } else {
            mBinding?.footerBar?.btnRight?.visibility = View.INVISIBLE
            startFdr()
        }
    }

    private fun initViewModelObservers() {
        faceIdentificationViewModel.initialLoad.observe(this, Observer { state ->
            when (state) {
                NetworkState.LOADING -> {}
                NetworkState.LOADED -> {}
                is NetworkState.error -> {}
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
            if (mode == SharedViewModel.MODE_FACE_IDENTIFICATION) {
                doSelfRestart()
            }
        })

        sharedViewModel.verifyFinishEvent.observe(this, Observer {
            currentState = VERIFY_FACE_FINISH_STATE
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
//                        when (sharedViewModel.clockModule) {
//                            SharedViewModel.MODULE_VISITOR -> mBinding?.visitorRegisterForm?.showSecurityCheckResult(true)
//                            else -> mBinding?.employeeRegisterForm?.showSecurityCheckResult(true)
//                        }
                    }

                    RegisterFormState.INVALID_SECURITY_CODE -> {
                        Timber.d("RegisterFormState.INVALID_SECURITY_CODE")
//                        when (sharedViewModel.clockModule) {
//                            SharedViewModel.MODULE_VISITOR -> mBinding?.visitorRegisterForm?.showSecurityCheckResult(false)
//                            else -> mBinding?.employeeRegisterForm?.showSecurityCheckResult(false)
//                        }
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
        SimpleRxTask.afterOnMain(500L) {
            mBinding?.fdrFrame?.addView(mFdrManager.mFdrCtrl)
        }
    }

    override fun stopFdr() {
        Timber.d("stopFdr()")
        super.stopFdr()
        changeFootBarUI()

        mFdrManager.stopFdr()
        mBinding?.fdrFrame?.removeAllViews()
        mBinding?.fdrFrame?.visibility = View.GONE
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
        if (sharedViewModel.isOptionClockMode()
            && mBinding?.footerBar?.failTextView?.visibility == View.GONE) {
            // this statement means UI is in the clock option mode
            startScanFace()
        } else {
            DeviceUtils.mFacePngList.clear()
            currentState = VERIFY_FACE_RUNNING_STATE

            updateStateUI(Constants.REGISTER_STATE_FACE_GET)
            changeFootBarUI()
            mFdrManager.restartScan()
        }

        mBinding?.footerBar?.successTextView?.visibility = View.GONE
        mBinding?.footerBar?.failTextView?.visibility = View.GONE
    }

    override fun onClickLeftBtn() {
        Timber.d("Click left button on state: $currentState")

        sharedViewModel.fdrResultVisibilityEvent.postValue(View.GONE)

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
                if (AppFeatureManager.IS_SUPPORT_RETRAIN_FEATURE) {
                    (activity as MainActivity).showRetrainPage()
                }
            }
        }
    }

    private fun updateStateUI(state: Int) {
        if (mPreferences.applicationMode == Constants.REGISTER_MODE) {
            //mBinding?.registerState = state
        }

        if (sharedViewModel.isSingleModuleMode()) {
            when (state) {
                Constants.REGISTER_STATE_COMPLETE -> {
                    //mBinding?.footerBar?.btnLeft?.visibility = View.VISIBLE
                    mBinding?.footerBar?.leftBtnText = getString(R.string.confirm)
                }

                else -> {
                    mBinding?.footerBar?.btnLeft?.visibility = View.INVISIBLE
                }
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