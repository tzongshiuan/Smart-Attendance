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
import kotlinx.android.synthetic.main.activity_main.view.*
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

    private var countDownDisposoble: Disposable? = null
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

    private fun updateLayoutUI() {
        // Change margin
        when (mPreferences.applicationMode) {
            Constants.REGISTER_MODE -> {
                val newLayoutParams1 = mBinding?.unknownResultLayout?.layoutParams as LayoutParams
                newLayoutParams1.topMargin = resources.getDimension(R.dimen.fdr_result_margin).toInt()
                mBinding?.unknownResultLayout?.layoutParams = newLayoutParams1

                val newLayoutParams2 = mBinding?.successResultLayout?.layoutParams as LayoutParams
                newLayoutParams2.topMargin = resources.getDimension(R.dimen.fdr_result_margin).toInt()
                mBinding?.successResultLayout?.layoutParams = newLayoutParams2

                mBinding?.dateTimeGroup?.visibility = View.GONE
            }

            Constants.VERIFICATION_MODE -> {
                val newLayoutParams1 = mBinding?.unknownResultLayout?.layoutParams as LayoutParams
                newLayoutParams1.topMargin = 0
                mBinding?.unknownResultLayout?.layoutParams = newLayoutParams1

                val newLayoutParams2 = mBinding?.successResultLayout?.layoutParams as LayoutParams
                newLayoutParams2.topMargin = 0
                mBinding?.successResultLayout?.layoutParams = newLayoutParams2

                mBinding?.dateTimeGroup?.visibility = View.VISIBLE
            }
        }
    }

    fun setRetrainModeUI(sharedViewModel: SharedViewModel, preferences: PreferencesHelper) {
        Timber.d("setRetrainModeUI()")
        mPreferences = preferences
        updateLayoutUI()

        mBinding?.unknownResultLayout?.visibility = View.GONE
        mBinding?.successResultLayout?.visibility = View.GONE
        mBinding?.checkOptionLayout?.visibility = View.GONE
        mBinding?.retrainLayout?.visibility = View.VISIBLE
    }

    fun setFailedUI(sharedViewModel: SharedViewModel, preferences: PreferencesHelper) {
        Timber.d("setFailedUI(), module: ${sharedViewModel.clockModule}, check mode: ${preferences.checkMode}")
        mPreferences = preferences

        updateLayoutUI()

        mBinding?.unknownResultLayout?.visibility = View.VISIBLE
        mBinding?.successResultLayout?.visibility = View.GONE
        mBinding?.checkOptionLayout?.visibility = View.GONE
        mBinding?.retrainLayout?.visibility = View.GONE

        startHoldResultCountDown()
    }

    fun setVerifySuccessUI(sharedViewModel: SharedViewModel, preferences: PreferencesHelper) {
        Timber.d("setVerifySuccessUI(), module: ${sharedViewModel.clockModule}, check mode: ${preferences.checkMode}")
        mPreferences = preferences

        updateLayoutUI()

        mBinding?.unknownResultLayout?.visibility = View.GONE
        mBinding?.successResultLayout?.visibility = View.VISIBLE
        mBinding?.retrainLayout?.visibility = View.GONE

        if (sharedViewModel.clockModule == SharedViewModel.MODULE_VISITOR) {
            /**
             * Visitor
             */
            when (preferences.checkMode) {
                Constants.CHECK_IN -> {
                    mBinding?.checkOptionLayout?.visibility = View.GONE
                    mBinding?.timeoutTextView?.visibility = View.INVISIBLE
                    clockTypeLiveEvent.postValue(CLOCK_ARRIVE)
                    startHoldResultCountDown()
                }

                Constants.CHECK_OUT -> {
                    mBinding?.checkOptionLayout?.visibility = View.GONE
                    mBinding?.timeoutTextView?.visibility = View.INVISIBLE
                    clockTypeLiveEvent.postValue(CLOCK_LEAVE)
                    startHoldResultCountDown()
                }

                Constants.CHECK_OPTION -> {
                    mBinding?.checkOptionLayout?.visibility = View.VISIBLE
                    mBinding?.employeeOptionGroup?.visibility = View.GONE
                    mBinding?.visitorOptionGroup?.visibility = View.VISIBLE

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
                    mBinding?.checkOptionLayout?.visibility = View.GONE
                    mBinding?.timeoutTextView?.visibility = View.INVISIBLE
                    clockTypeLiveEvent.postValue(CLOCK_CHECK_IN)
                    startHoldResultCountDown()
                }

                Constants.CHECK_OUT -> {
                    mBinding?.checkOptionLayout?.visibility = View.GONE
                    mBinding?.timeoutTextView?.visibility = View.INVISIBLE
                    clockTypeLiveEvent.postValue(CLOCK_CHECK_OUT)
                    startHoldResultCountDown()
                }

                Constants.CHECK_OPTION -> {
                    mBinding?.checkOptionLayout?.visibility = View.VISIBLE
                    mBinding?.employeeOptionGroup?.visibility = View.VISIBLE
                    mBinding?.visitorOptionGroup?.visibility = View.GONE

                    // start user clock timeout
                    mBinding?.timeoutTextView?.visibility = View.VISIBLE
                    startInterActionCountDown()
                }
            }
        }
    }

    fun setRegisterSuccessUI(sharedViewModel: SharedViewModel, registerViewModel: RegisterViewModel
                             , preferences: PreferencesHelper) {
        Timber.d("setVerifySuccessUI(), module: ${sharedViewModel.clockModule}")
        mPreferences = preferences

        updateLayoutUI()

        when (sharedViewModel.clockModule) {
            SharedViewModel.MODULE_VISITOR -> {
                mBinding?.userNameText?.text = String.format("%s: %s",
                    resources.getString(R.string.result_visitor_name), registerViewModel.visitorRegisterData?.name)
                mBinding?.welcomeLabel?.text = String.format("%s: %s",
                    resources.getString(R.string.result_visitor_phone), registerViewModel.visitorRegisterData?.mobileNo)
            }

            else -> {
                mBinding?.userNameText?.text = String.format("%s: %s",
                    resources.getString(R.string.result_employee_name), registerViewModel.employeeRegisterData?.name)
                mBinding?.welcomeLabel?.text = String.format("%s: %s",
                    resources.getString(R.string.result_employee_id), registerViewModel.employeeRegisterData?.employeeId)
            }
        }

        mBinding?.unknownResultLayout?.visibility = View.GONE
        mBinding?.successResultLayout?.visibility = View.VISIBLE
        mBinding?.checkOptionLayout?.visibility = View.GONE
        mBinding?.timeoutTextView?.visibility = View.INVISIBLE
        mBinding?.retrainLayout?.visibility = View.GONE

        startHoldResultCountDown()
    }

    /**
     * User verified result will only show for a while
     */
    private fun startHoldResultCountDown() {
        Timber.d("startHoldResultCountDown()")

        countDownDisposoble = RxCountDownTimer.create(mPreferences.holdResultTime, mPreferences.holdResultTime)
            .subscribe {millisUntilFinished ->
                if (millisUntilFinished == 0L) {
                    Timber.d("CountDown: $millisUntilFinished (milli-secs)")
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
        countDownDisposoble = RxCountDownTimer.create(DeviceUtils.CHOOSE_CLOCK_TYPE_TIME, 500)
            .subscribe {millisUntilFinished ->
                Timber.d("CountDown: $millisUntilFinished (milli-secs)")

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
                .subscribe {
                    synchronized(isCountDownStart) {
                        if (isCountDownStart) {
                            isCountDownStart = false
                            countDownDisposoble?.dispose()
                            clockTypeLiveEvent.postValue(CLOCK_CHECK_IN)
                            hideFaceResultView.postValue(true)
                        }
                    }
                }
        }

        mBinding?.checkOutBtn?.let {
            RxView.clicks(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .subscribe {
                    synchronized(isCountDownStart) {
                        if (isCountDownStart) {
                            isCountDownStart = false
                            countDownDisposoble?.dispose()
                            clockTypeLiveEvent.postValue(CLOCK_CHECK_OUT)
                            hideFaceResultView.postValue(true)
                        }
                    }
                }
        }

        mBinding?.passBtn?.let {
            RxView.clicks(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .subscribe {
                    synchronized(isCountDownStart) {
                        if (isCountDownStart) {
                            isCountDownStart = false
                            countDownDisposoble?.dispose()
                            clockTypeLiveEvent.postValue(CLOCK_PASS)
                            hideFaceResultView.postValue(true)
                        }
                    }
                }
        }

        mBinding?.arriveBtn?.let {
            RxView.clicks(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .subscribe {
                    synchronized(isCountDownStart) {
                        if (isCountDownStart) {
                            isCountDownStart = false
                            countDownDisposoble?.dispose()
                            clockTypeLiveEvent.postValue(CLOCK_ARRIVE)
                            hideFaceResultView.postValue(true)
                        }
                    }
                }
        }

        mBinding?.leaveBtn?.let {
            RxView.clicks(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .subscribe {
                    synchronized(isCountDownStart) {
                        if (isCountDownStart) {
                            isCountDownStart = false
                            countDownDisposoble?.dispose()
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
            countDownDisposoble?.dispose()
        }
    }
}