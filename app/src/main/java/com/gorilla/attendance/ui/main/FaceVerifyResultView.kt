package com.gorilla.attendance.ui.main

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.gorilla.attendance.R
import com.gorilla.attendance.databinding.FaceVerifyResultLayoutBinding
import com.gorilla.attendance.ui.common.SharedViewModel
import com.gorilla.attendance.ui.register.RegisterViewModel
import com.gorilla.attendance.utils.*
import com.gorilla.attendance.utils.rxCountDownTimer.RxCountDownTimer
import com.jakewharton.rxbinding.view.RxView
import io.reactivex.disposables.Disposable
import rx.android.schedulers.AndroidSchedulers
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/7/3
 * Description: Kotlin version of progress view (by Shawn Wang)
 */
open class FaceVerifyResultView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    companion object {
//        @JvmStatic
//        @BindingAdapter("showControlImgSrc")
//        fun showControlImgSrc(view: View, isExpandControlView: Boolean) {
//        }

        const val CLOCK_CHECK_IN = 0
        const val CLOCK_CHECK_OUT = 1
        const val CLOCK_PASS = 2
        const val CLOCK_ARRIVE = 3
        const val CLOCK_LEAVE = 4
        const val CLOCK_TIMEOUT = 5
        const val CLOCK_HOLD_RESULT_TIMEOUT = 6
    }

    private var mBinding: FaceVerifyResultLayoutBinding? = null

    private var countDownDisposable: Disposable? = null
    private var isCountDownStart = false

    private lateinit var mPreferences: PreferencesHelper

    val clockTypeLiveEvent = SingleLiveEvent<Int>()

    val hideFaceResultView = SingleLiveEvent<Boolean>()

    var unknownLabel: String? = null
        set(value) {
            if (value != null) {
                mBinding?.failedNameText = value
            }
            field = value
        }

    var failedLabel: String? = null
        set(value) {
            if (value != null) {
                mBinding?.failedLabelText = value
            }
            field = value
        }

    var failedText: String? = null
        set(value) {
            if (value != null) {
                mBinding?.failedText = value
            }
            field = value
        }

    var userImage: ByteArray? = null
        set(value) {
            if (value != null) {
                mBinding?.userImage?.setImageBitmap(
                    ImageUtils.getBitmapFromBytes(value)
                )
            }
            field = value
        }

    var registerUserImage: ByteArray? = null
        set(value) {
            if (value != null) {
                mBinding?.registerUserImage?.setImageBitmap(
                    ImageUtils.getBitmapFromBytes(value)
                )
            }
            field = value
        }

    var userName: String? = null
        set(value) {
            if (value != null) {
                mBinding?.userName = value
            }
            field = value
        }

    var successLabel: String? = null
        set(value) {
            if (value != null) {
                mBinding?.successLabelText = value
            }
            field = value
        }

    var infoText: String? = null
        set(value) {
            if (value != null) {
                mBinding?.infoText = value
            }
            field = value
        }

    var clockTime: Date? = null
        set(value) {
            if (value != null) {
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                mBinding?.clockText = sdf.format(value)
            }
            field = value
        }

    var calendarTime: Date? = null
        set(value) {
            if (value != null) {
                val sdf = SimpleDateFormat("yyyy-MM-dd EEE", Locale.getDefault())
                mBinding?.calendarText = sdf.format(value)
            }
            field = value
        }


    init {
        initView(context)
    }

    private fun initView(context: Context) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        //inflater.inflate(R.layout.face_verify_result_layout, this)
        mBinding = FaceVerifyResultLayoutBinding.inflate(inflater, this, true)

        initListeners()
    }

    fun setRetrainModeUI(sharedViewModel: SharedViewModel, preferences: PreferencesHelper) {
        Timber.d("setRetrainModeUI(), module: ${sharedViewModel.clockModule}")

        mPreferences = preferences

        mBinding?.unknownResultLayout?.visibility = View.GONE
        mBinding?.registerLayout?.visibility = View.GONE
        mBinding?.successResultLayout?.visibility = View.GONE
        mBinding?.retrainLayout?.visibility = View.VISIBLE
    }

    fun setFailedUI(sharedViewModel: SharedViewModel, preferences: PreferencesHelper) {
        Timber.d("setFailedUI(), module: ${sharedViewModel.clockModule}, check mode: ${preferences.checkMode}")
        mPreferences = preferences

        mBinding?.unknownResultLayout?.visibility = View.VISIBLE
        mBinding?.registerLayout?.visibility = View.GONE
        mBinding?.successResultLayout?.visibility = View.GONE
        mBinding?.retrainLayout?.visibility = View.GONE

        startHoldResultCountDown()
    }

    fun setVerifySuccessUI(sharedViewModel: SharedViewModel, preferences: PreferencesHelper) {
        Timber.d("setVerifySuccessUI(), module: ${sharedViewModel.clockModule}, check mode: ${preferences.checkMode}")
        mPreferences = preferences

        mBinding?.unknownResultLayout?.visibility = View.GONE
        mBinding?.registerLayout?.visibility = View.GONE
        mBinding?.successResultLayout?.visibility = View.VISIBLE
        mBinding?.retrainLayout?.visibility = View.GONE

        if (sharedViewModel.clockModule == SharedViewModel.MODULE_VISITOR) {
            /**
             * Visitor
             */
            when (preferences.checkMode) {
                Constants.CHECK_IN -> {
                    mBinding?.timeoutTextView?.visibility = View.INVISIBLE
                    clockTypeLiveEvent.postValue(CLOCK_ARRIVE)
                    startHoldResultCountDown()
                }

                Constants.CHECK_OUT -> {
                    mBinding?.timeoutTextView?.visibility = View.INVISIBLE
                    clockTypeLiveEvent.postValue(CLOCK_LEAVE)
                    startHoldResultCountDown()
                }

                Constants.CHECK_OPTION -> {
                    mBinding?.isShowEmployeeBtn = false
                    mBinding?.isShowVisitorBtn = true

                    // start user clock timeout
                    mBinding?.timeoutTextView?.visibility = View.VISIBLE
                    startInterActionCountDown()
                }
            }
        } else {
            /**
             * Employee
             */
            when (preferences.checkMode) {
                Constants.CHECK_IN -> {
                    mBinding?.timeoutTextView?.visibility = View.INVISIBLE
                    clockTypeLiveEvent.postValue(CLOCK_CHECK_IN)
                    startHoldResultCountDown()
                }

                Constants.CHECK_OUT -> {
                    mBinding?.timeoutTextView?.visibility = View.INVISIBLE
                    clockTypeLiveEvent.postValue(CLOCK_CHECK_OUT)
                    startHoldResultCountDown()
                }

                Constants.CHECK_OPTION -> {
                    mBinding?.isShowEmployeeBtn = true
                    mBinding?.isShowVisitorBtn = false

                    // start user clock timeout
                    mBinding?.timeoutTextView?.visibility = View.VISIBLE
                    startInterActionCountDown()
                }
            }
        }
    }

    fun setRegisterSuccessUI(sharedViewModel: SharedViewModel, registerViewModel: RegisterViewModel
                             , preferences: PreferencesHelper) {
        Timber.d("setRegisterSuccessUI(), module: ${sharedViewModel.clockModule}")
        mPreferences = preferences

        when (sharedViewModel.clockModule) {
            SharedViewModel.MODULE_VISITOR -> {
                mBinding?.registerUserNameText?.text = String.format("%s: %s",
                    resources.getString(R.string.result_visitor_name), registerViewModel.visitorRegisterData?.name)
                mBinding?.registerWelcomeLabel?.text = String.format("%s: %s",
                    resources.getString(R.string.result_visitor_phone), registerViewModel.visitorRegisterData?.mobileNo)
            }

            else -> {
                mBinding?.registerUserNameText?.text = String.format("%s: %s",
                    resources.getString(R.string.result_employee_name), registerViewModel.employeeRegisterData?.name)
                mBinding?.registerWelcomeLabel?.text = String.format("%s: %s",
                    resources.getString(R.string.result_employee_id), registerViewModel.employeeRegisterData?.employeeId)
            }
        }

        mBinding?.unknownResultLayout?.visibility = View.GONE
        mBinding?.successResultLayout?.visibility = View.GONE
        mBinding?.retrainLayout?.visibility = View.GONE
        mBinding?.isShowEmployeeBtn = false
        mBinding?.isShowVisitorBtn = false

        mBinding?.registerLayout?.visibility = View.VISIBLE

        startHoldResultCountDown()
    }

    /**
     * User verified result will only show for a while
     */
    private fun startHoldResultCountDown() {
        Timber.d("startHoldResultCountDown()")

        countDownDisposable = RxCountDownTimer.create(mPreferences.holdResultTime, mPreferences.holdResultTime)
            .subscribe {millisUntilFinished ->
                if (millisUntilFinished == 0L) {
                    Timber.i("CountDown: $millisUntilFinished (milli-secs)")
                    clockTypeLiveEvent.postValue(CLOCK_HOLD_RESULT_TIMEOUT)
                }
            }
    }

    /**
     * User must select clock type if in clock option mode
     */
    private fun startInterActionCountDown() {
        Timber.d("startInterActionCountDown()")

        isCountDownStart = true
        countDownDisposable = RxCountDownTimer.create(DeviceUtils.CHOOSE_CLOCK_TYPE_TIME, 500)
            .subscribe {millisUntilFinished ->
                Timber.i("CountDown: $millisUntilFinished (milli-secs)")

                val text = String.format(context.getString(R.string.face_timeout_str_fmt), millisUntilFinished/1000)
                mBinding?.timeoutTextView?.text = text

                synchronized(isCountDownStart) {
                    if (millisUntilFinished == 0L && isCountDownStart) {
                        isCountDownStart = false
                        clockTypeLiveEvent.postValue(CLOCK_TIMEOUT)
                    }
                }
            }
    }

    /**
     * Initial all of the UI listeners here
     */
    private fun initListeners() {
        mBinding?.checkInBtn?.let {
            RxView.clicks(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    synchronized(isCountDownStart) {
                        if (isCountDownStart) {
                            isCountDownStart = false
                            countDownDisposable?.dispose()
                            clockTypeLiveEvent.postValue(CLOCK_CHECK_IN)
                            hideFaceResultView.postValue(true)
                        }
                    }
                }
        }

        mBinding?.checkOutBtn?.let {
            RxView.clicks(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    synchronized(isCountDownStart) {
                        if (isCountDownStart) {
                            isCountDownStart = false
                            countDownDisposable?.dispose()
                            clockTypeLiveEvent.postValue(CLOCK_CHECK_OUT)
                            hideFaceResultView.postValue(true)
                        }
                    }
                }
        }

        mBinding?.passBtn?.let {
            RxView.clicks(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    synchronized(isCountDownStart) {
                        if (isCountDownStart) {
                            isCountDownStart = false
                            countDownDisposable?.dispose()
                            clockTypeLiveEvent.postValue(CLOCK_PASS)
                            hideFaceResultView.postValue(true)
                        }
                    }
                }
        }

        mBinding?.arriveBtn?.let {
            RxView.clicks(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    synchronized(isCountDownStart) {
                        if (isCountDownStart) {
                            isCountDownStart = false
                            countDownDisposable?.dispose()
                            clockTypeLiveEvent.postValue(CLOCK_ARRIVE)
                            hideFaceResultView.postValue(true)
                        }
                    }
                }
        }

        mBinding?.leaveBtn?.let {
            RxView.clicks(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    synchronized(isCountDownStart) {
                        if (isCountDownStart) {
                            isCountDownStart = false
                            countDownDisposable?.dispose()
                            clockTypeLiveEvent.postValue(CLOCK_LEAVE)
                            hideFaceResultView.postValue(true)
                        }
                    }
                }
        }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)

        if (visibility == View.GONE) {
            countDownDisposable?.dispose()
        }
    }
}