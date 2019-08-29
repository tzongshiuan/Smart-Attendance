package com.gorilla.attendance.ui.main

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.gorilla.attendance.AttendanceApp
import com.gorilla.attendance.R
import com.gorilla.attendance.api.HostSelectionInterceptor
import com.gorilla.attendance.data.model.DeviceLoginData
import com.gorilla.attendance.data.model.NetworkState
import com.gorilla.attendance.data.model.Status
import com.gorilla.attendance.databinding.ActivityMainBinding
import com.gorilla.attendance.di.Injectable
import com.gorilla.attendance.ui.chooseMember.ChooseMemberFragment
import com.gorilla.attendance.ui.chooseMode.ChooseModeFragment
import com.gorilla.attendance.ui.common.BaseFragment
import com.gorilla.attendance.ui.common.SharedViewModel
import com.gorilla.attendance.ui.faceIdentification.FaceIdentificationFragment
import com.gorilla.attendance.ui.qrCode.QrCodeFragment
import com.gorilla.attendance.ui.register.RegisterViewModel
import com.gorilla.attendance.ui.rfid.RFIDFragment
import com.gorilla.attendance.ui.screenSaver.ScreenSaverFragment
import com.gorilla.attendance.ui.securityCode.SecurityCodeFragment
import com.gorilla.attendance.utils.*
import com.gorilla.attendance.utils.networkChecker.NetworkChecker
import com.gorilla.attendance.viewModel.AttendanceViewModelFactory
import com.jakewharton.rxbinding.view.RxView
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import rx.android.schedulers.AndroidSchedulers
import timber.log.Timber
import java.util.Locale
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.system.exitProcess


class MainActivity @Inject constructor() : AppCompatActivity(), HasSupportFragmentInjector, Injectable, SensorEventListener {

    companion object {
        private const val PERMISSION_ALL = 1

        private const val REQUEST_ENABLE_BT = 1001

        // for test and development convenience
        var IS_DEBUG_GOGO = false

        var IS_SKIP_FDR = false
            get() {
                if (IS_DEBUG_GOGO) {
                    return field
                } else {
                    return false
                }
            }
        var USE_TEST_FACE = false
            get() {
                if (IS_DEBUG_GOGO) {
                    return field
                } else {
                    return false
                }
            }

        var SEND_CLOCK_EVENT = false
            get() {
                if (IS_DEBUG_GOGO) {
                    return field
                } else {
                    return true
                }
            }
        var GET_DEVICE_IDENTITIES = false
            get() {
                if (IS_DEBUG_GOGO) {
                    return field
                } else {
                    return true
                }
            }

        var IS_ENABLE_SCREEN_SAVER = false
            get() {
                if (IS_DEBUG_GOGO) {
                    return field
                } else {
                    return true
                }
            }

        var testSecurityCode = "07121234"
        var IS_SKIP_SCAN_CODE = false
            get() {
                if (IS_DEBUG_GOGO) {
                    return field
                } else {
                    return false
                }
            }
    }

    @Inject
    lateinit var factory: AttendanceViewModelFactory

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var mPreferences: PreferencesHelper

    @Inject
    lateinit var mFdrManager: FdrManager

    @Inject
    lateinit var mObbManager: ObbManager

    @Inject
    lateinit var mWebSocketManager: WebSocketManager

    @Inject
    lateinit var mBtLeManager: BtLeManager

    @Inject
    lateinit var mNetworkChecker: NetworkChecker

    @Inject
    lateinit var mUsbRelayManager: UsbRelayManager

    @Inject
    lateinit var mInterceptor: HostSelectionInterceptor

    private var mBinding: ActivityMainBinding? = null

    private lateinit var mainViewModel: MainViewModel
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var registerViewModel: RegisterViewModel

    private var enterSettingCount = 0
    private var enterSettingClickTime = 0L

    private var isInitObbManager = false

    private var navHomeId: Int? = null
    private var navHomeLabel: String? = null
    private lateinit var navController: NavController

    private lateinit var mSensorManager: SensorManager
    private var isHaveSensor = false
    @Volatile private var mRotation = 0

    /**
     * Navigate to home page or screen saver page
     */
    private var screenSaverSubscriber: Disposable? = null


    private val mPermissions = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.RECEIVE_BOOT_COMPLETED,
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate()")

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setSupportActionBar(mBinding?.toolbar as Toolbar)

        mainViewModel = ViewModelProviders.of(this, factory).get(MainViewModel::class.java)
        sharedViewModel = ViewModelProviders.of(this, factory).get(SharedViewModel::class.java)
        registerViewModel = ViewModelProviders.of(this, factory).get(RegisterViewModel::class.java)

        mPreferences.readPreferences()

        navController = (container as NavHostFragment).navController

        initViewModelObservers()
        initUI()

        if(!hasPermissions()){
            ActivityCompat.requestPermissions(this, mPermissions, PERMISSION_ALL)
        } else {
            initSetting()
        }

        mRotation = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensors = mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION)
        if (sensors.size > 0) {
            isHaveSensor = true
            mSensorManager.registerListener(this, sensors[0], SensorManager.SENSOR_DELAY_NORMAL)
        }

        // test
//        Crashlytics.setInt("priority", Log.ERROR)
//        Crashlytics.setString("tag", "TAGTAGTAG")
//        Crashlytics.setString("message", "message message message")
//        Crashlytics.logException(Exception("message message message"))
//        Crashlytics.getInstance().crash()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        Timber.d("onCreateOptionsMenu()")
        return super.onCreateOptionsMenu(menu)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val rotation = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation

        if (rotation != mRotation && (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)) {
            mRotation = rotation
            mPreferences.languageId?.let {
                when (it) {
                    MainViewModel.LANGUAGE_CH_SIM -> mBinding?.languageImage?.setImageResource(R.mipmap.ic_ch_simplified)
                    MainViewModel.LANGUAGE_CH_TRA -> mBinding?.languageImage?.setImageResource(R.mipmap.ic_ch_traditional)
                    else -> mBinding?.languageImage?.setImageResource(R.mipmap.ic_en)
                }
                mainViewModel.changeLanguageConfig(this, mPreferences.languageId ?: MainViewModel.LANGUAGE_EN)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        Timber.d("onOptionsItemSelected()")
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        Timber.d("onStart()")
        super.onStart()
    }

    override fun onResume() {
        Timber.d("onResume()")
        super.onResume()

        AttendanceApp.activityResumed()
    }

    override fun onPause() {
        Timber.d("onPause()")
        super.onPause()

        AttendanceApp.activityPaused()

        if (isInitObbManager) {
            mObbManager.stop()
        }

        mBtLeManager.onActivityPause()

        mPreferences.savePreferences()
    }

    override fun onStop() {
        Timber.d("onStop()")
        super.onStop()
    }

    override fun onDestroy() {
        Timber.d("onDestroy()")
        super.onDestroy()

        stopSubscriber()

        if (isHaveSensor) {
            mSensorManager.unregisterListener(this)
        }

        mWebSocketManager.disconnect()

        mBtLeManager.onActivityDestroy()

        mUsbRelayManager.stop()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        //Timber.d("onUserInteraction()")

        //if (mPreferences.isDeviceDbExist) {
            startSubscriber()
        //}
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Timber.d("onBackPressed()")

        mBinding?.faceVerifyResultView?.visibility = View.GONE
    }

    private fun startScreenSaverSubscriber() {
        if (!mPreferences.isScreenSaverEnable || !IS_ENABLE_SCREEN_SAVER) {
            SimpleRxTask.cancelSubscriber(screenSaverSubscriber)
            return
        }
        //Timber.d("startScreenSaverSubscriber()")

        screenSaverSubscriber = SimpleRxTask.createDelaySubscriber(mPreferences.idleTime * 1000) {
            /**
             * If not in screen saver page
             */
            if (navController.currentDestination?.label != ScreenSaverFragment::class.java.simpleName) {
                // back home first
                if (BaseFragment.currentState == BaseFragment.VERIFY_FACE_RUNNING_STATE) {
                    startSubscriber()
                    return@createDelaySubscriber
                }

                if (navController.currentDestination?.id == R.id.settingFragment) {
                    startSubscriber()
                    return@createDelaySubscriber
                }

                // Back to home page
                navHomeId?.let { id ->
                    if (navController.currentDestination?.label != navHomeLabel) {
                        Timber.d("Navigate to home page !!")
                        navController.popBackStack(id, false)
                        SimpleRxTask.onMain {
                            mBinding?.faceVerifyResultView?.visibility = View.GONE
                        }
                    }
                }
                startSubscriber()

                try {
                    // Navigate to screen saver page
                    val navBuilder = NavOptions.Builder()
                    navBuilder.setEnterAnim(R.anim.fade_in)
                    navBuilder.setPopExitAnim(R.anim.fade_out)
                    navController.navigate(R.id.screenSaverFragment, null, navBuilder.build())
                } catch (e: Exception) {
                    Timber.e("Navigate to screen saver page exception, message: ${e.message}")
                }
            }
        }
    }

    private fun startBackHomeSubscriber() {
        if (!mPreferences.isScreenSaverEnable || !IS_ENABLE_SCREEN_SAVER) {
            SimpleRxTask.cancelSubscriber(screenSaverSubscriber)
            return
        }
        Timber.d("startBackHomeSubscriber()")

        screenSaverSubscriber = SimpleRxTask.createDelaySubscriber(DeviceUtils.BACK_TO_HOME_DELAYED_TIME) {

            if (BaseFragment.currentState == BaseFragment.VERIFY_FACE_RUNNING_STATE) {
                startBackHomeSubscriber()
                return@createDelaySubscriber
            }

            // Back to home page
            navHomeId?.let { id ->
                if (navController.currentDestination?.label != navHomeLabel) {
                    Timber.d("Navigate to home page !!")
                    navController.popBackStack(id, false)
                    mBinding?.faceVerifyResultView?.visibility = View.GONE
                    startSubscriber()
                } else {
                    startSubscriber(true)
                }
            }
        }
    }

    fun startSubscriber(isForceScreenSaver: Boolean = false) {
        if (!mPreferences.isScreenSaverEnable || !IS_ENABLE_SCREEN_SAVER) {
            SimpleRxTask.cancelSubscriber(screenSaverSubscriber)
            return
        }

        SimpleRxTask.cancelSubscriber(screenSaverSubscriber)

        when {
            navController.currentDestination?.label == ScreenSaverFragment::class.java.simpleName -> {
                Timber.d("Already in the screen saver page")
                navController.popBackStack()    // to home page
//                startScreenSaverSubscriber()
            }

//            navController.currentDestination?.label == navHomeLabel -> {
//                if (isForceScreenSaver) {
//                    startScreenSaverSubscriber()
//                } else {
//                    startBackHomeSubscriber()
//                }
//            }
//
//            else -> startBackHomeSubscriber()
        }

        startScreenSaverSubscriber()
    }

    private fun stopSubscriber() {
        SimpleRxTask.cancelSubscriber(screenSaverSubscriber)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.d("onActivityResule(), resultCode = $resultCode, requestCode = $requestCode")

        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if (resultCode == Activity.RESULT_OK) {
                    Timber.d("Bluetooth enable success")

                    mBtLeManager.initSetting(this)
                } else {
                    Timber.d("Bluetooth still disable now")
                    Toast.makeText(this, "Declined to enable Bluetooth, force to close app", Toast.LENGTH_LONG).show()
                    SimpleRxTask.afterOnMain(Constants.PERMISSION_DENY_CLOSE_TIME) {
                        this.finish()
                    }
                }
            }
        }
    }

    private fun turnBluetoothOn() {
        Timber.d("turnBluetoothOn")

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        Timber.d("bluetoothAdapter.isEnabled() = %s", bluetoothAdapter.isEnabled)
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    fun sendFailedClockEvent() {
        mainViewModel.sendFailedClockEvent()
    }

    // Show retrain mode UI
    fun showRetrainPage() {
        Timber.d("showRetrainPage()")
        mBinding?.faceVerifyResultView?.setRetrainModeUI(sharedViewModel, mPreferences)

        mBinding?.faceVerifyResultView?.userImage = DeviceUtils.mFacePngList[0]

        //mBinding?.faceVerifyResultView?.userName = sharedViewModel.getFullName()
        //mBinding?.faceVerifyResultView?.infoText = "Information information information information information"
    }

    fun initFdr() {
        mFdrManager.initFdr(this, factory)
    }

    fun navigateToHome() {
        // Back to home page
        navHomeId?.let { id ->
            if (navController.currentDestination?.label != navHomeLabel) {
                Timber.d("Navigate to home page !!")
                navController.popBackStack(id, false)
            }
        }
    }

    fun applyNewSetting() {
        Timber.d("apply new setting()")

        mPreferences.applyNewSetting()

        navigateToBaseFragment()

        // If network is available, the system must update DB after setting was changed
        //if (mNetworkChecker.isNetworkAvailable()) {
        mPreferences.isDeviceDbExist = false
        sharedViewModel.deviceName = null
        //}

        // To ensure RTSP camera setting is synchronized

        mainViewModel.stopClockEventSendingJob()
        SimpleRxTask.onMain {
            mBinding?.titleText?.text = ""
            initSettingFinished()
        }
    }

    fun setToolbarVisible(visible: Boolean) {
        if (visible) {
            if (!QrCodeFragment.isRtspClientRunning) {
                mBinding?.toolbar?.visibility = View.VISIBLE
                mBinding?.toolbarBottomLine?.visibility = View.VISIBLE
            }
        } else  {
            mBinding?.toolbar?.visibility = View.GONE
            mBinding?.toolbarBottomLine?.visibility = View.GONE
        }
    }

    private fun hasPermissions(): Boolean {
        for (permission in mPermissions) {
            if (this.let { ContextCompat.checkSelfPermission(it, permission) } != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_ALL -> {
                var isAllGrant = true
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        isAllGrant = false
                        break
                    }
                }

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && isAllGrant) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    initSetting()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Request all permissions failed, force to close app", Toast.LENGTH_LONG).show()
                    SimpleRxTask.afterOnMain(Constants.PERMISSION_DENY_CLOSE_TIME) {
                        this.finish()
                    }
                }
                return
            }
        }
    }

    private fun initUI() {
        initToolbar()
    }

    private fun initToolbar() {
        mainViewModel.updateDateTime()

        if (mPreferences.languageId == null) {
            val language = Locale.getDefault().language
            val country = Locale.getDefault().country
            when {
                Locale.SIMPLIFIED_CHINESE.language == language
                && Locale.SIMPLIFIED_CHINESE.country == country -> {
                    mBinding?.languageImage?.setImageResource(R.mipmap.ic_ch_simplified)
                    mPreferences.languageId = MainViewModel.LANGUAGE_CH_SIM
                }
                Locale.TRADITIONAL_CHINESE.language == language
                && Locale.TRADITIONAL_CHINESE.country == country -> {
                    mBinding?.languageImage?.setImageResource(R.mipmap.ic_ch_traditional)
                    mPreferences.languageId = MainViewModel.LANGUAGE_CH_TRA
                }

                else -> {
                    mBinding?.languageImage?.setImageResource(R.mipmap.ic_en)
                    mPreferences.languageId = MainViewModel.LANGUAGE_EN
                }
            }
        } else {
            when (mPreferences.languageId) {
                MainViewModel.LANGUAGE_CH_SIM -> mBinding?.languageImage?.setImageResource(R.mipmap.ic_ch_simplified)
                MainViewModel.LANGUAGE_CH_TRA -> mBinding?.languageImage?.setImageResource(R.mipmap.ic_ch_traditional)
                else -> mBinding?.languageImage?.setImageResource(R.mipmap.ic_en)
            }
            mainViewModel.changeLanguageConfig(this, mPreferences.languageId ?: MainViewModel.LANGUAGE_EN)
        }

        mainViewModel.languageIdChangeEvent.observe(this, Observer { languageId ->
            when (languageId) {
                MainViewModel.LANGUAGE_CH_SIM -> mBinding?.languageImage?.setImageResource(R.mipmap.ic_ch_simplified)
                MainViewModel.LANGUAGE_CH_TRA -> mBinding?.languageImage?.setImageResource(R.mipmap.ic_ch_traditional)
                else -> mBinding?.languageImage?.setImageResource(R.mipmap.ic_en)
            }
            mPreferences.languageId = languageId
        })

        mBinding?.languageImage?.let {
            RxView.clicks(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    // get language select dialog from view model
                    mainViewModel.showLanguageSelectDialog(this)
                }
        }

        mBinding?.timeText?.let {
            RxView.clicks(it)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    // do nothing if already in the setting page
                    if (navController.currentDestination?.id == R.id.settingFragment) {
                        return@subscribe
                    }

                    // if loading, do nothing
                    if (mainViewModel.initialLoad.value == NetworkState.LOADING) {
                        return@subscribe
                    }

                    // if in fdr mode, do nothing for thread safety
                    if (BaseFragment.currentState == BaseFragment.VERIFY_FACE_RUNNING_STATE) {
                        return@subscribe
                    }

                    if (enterSettingCount >= DeviceUtils.ENTER_SETTING_CLICK_NUM
                        || (System.currentTimeMillis() - enterSettingClickTime) > DeviceUtils.ENTER_SETTING_INTERVAL_TIME) {
                        enterSettingCount = 0
                    }

                    if (enterSettingCount == 0) {
                        enterSettingClickTime = System.currentTimeMillis()
                    }

                    enterSettingCount++
                    Timber.d("enterSettingCount: $enterSettingCount")

                    if (enterSettingCount >= DeviceUtils.ENTER_SETTING_CLICK_NUM) {
                        Timber.d("Enough click counts, go to setting page")
                        enterSettingCount = 0

                        mBinding?.faceVerifyResultView?.visibility = View.GONE

                        try {
                            mBinding?.waitingView?.setVisibleImmediate(View.GONE)

                            val navBuilder = NavOptions.Builder()
                            navBuilder.setEnterAnim(R.anim.slide_bottom_in)
                            navBuilder.setPopExitAnim(R.anim.slide_bottom_out)
                            navController.navigate(R.id.settingFragment, null, navBuilder.build())
                        } catch (e: Exception) {
                            Timber.e("Navigate to setting page exception, message: ${e.message}")
                            navigateToBaseFragment()
                        }
                    }
                }
        }
    }

    private fun initViewModelObservers() {
        sharedViewModel.changeTitleEvent.observe(this, Observer { title ->
            Timber.d("setSubTitle(), sub title: $title")
            mBinding?.titleText?.text = title
        })

        sharedViewModel.fdrResultVisibilityEvent.observe(this, Observer { visibility ->
            Timber.d("faceVerifyResultView, visibility: $visibility")
            visibility?.let {
                mBinding?.faceVerifyResultView?.visibility = it
            }
        })

        mainViewModel.dateTimeData.observe(this, Observer { list ->
            if (list != null && list.size == 3) {
                mBinding?.timeText?.text = list[0]
                mBinding?.dateText?.text = list[1]
                mBinding?.weekDayText?.text = list[2]
            }
        })

        mainViewModel.initialLoad.observe(this, Observer { state ->
            Timber.d("initlial load, state: ${state?.status}")
            val visibility = if (state?.status == NetworkState.LOADING.status) View.VISIBLE else View.GONE
            mBinding?.waitingView?.setVisibleWithAnimate(visibility)

            if (state != null && state.status == Status.FAILED && state.msg != null) {
                Toast.makeText(this, state.msg, Toast.LENGTH_LONG).show()
                mBinding?.errorHintText?.visibility = View.VISIBLE
                stopSubscriber()
            } else if (state != null && state.status == Status.RUNNING) {
                //navigateToBaseFragment()
                mBinding?.errorHintText?.visibility = View.GONE
            }
        })


        /**
         * Verification
         */
        mainViewModel.faceVerifyErrorData.observe(this, Observer { errorData ->
            Timber.d("Face verify failed, code: ${errorData?.code}, message: ${errorData?.message}")
            Toast.makeText(this, "Face verify failed, code: ${errorData?.code}, " +
                    "message: ${errorData?.message}", Toast.LENGTH_LONG).show()
        })

        mainViewModel.faceVerifyUiMode.observe(this, Observer { uiMode ->
            mBinding?.waitingView?.setVisibleImmediate(View.GONE)
            mBinding?.faceVerifyResultView?.visibility = View.VISIBLE

            sharedViewModel.verifyFinishEvent.postValue(true)

            startSubscriber()

            when (uiMode) {
                Constants.UI_FACE_UNKNOWN -> {
                    // show failed UI
                    mBinding?.faceVerifyResultView?.setFailedUI(sharedViewModel, mPreferences)
                    mBinding?.faceVerifyResultView?.unknownLabel = getString(R.string.unknown_user)
                    mBinding?.faceVerifyResultView?.failedLabel = getString(R.string.verification_failed)
                    mBinding?.faceVerifyResultView?.failedText = "Description description description description description description description"
                    sendFailedClockEvent()
                }
                Constants.UI_FACE_VALID -> {
                    // show success UI with different types
                    mBinding?.faceVerifyResultView?.setVerifySuccessUI(sharedViewModel, mPreferences)
                    mBinding?.faceVerifyResultView?.userImage = DeviceUtils.mFacePngList[0]
                    mBinding?.faceVerifyResultView?.userName = sharedViewModel.getFullName()
                    mBinding?.faceVerifyResultView?.successLabel = getString(R.string.welcome_back)
                    mBinding?.faceVerifyResultView?.infoText = "Information information information information information"
                    val dateNow = Date()
                    mBinding?.faceVerifyResultView?.clockTime = dateNow
                    mBinding?.faceVerifyResultView?.calendarTime = dateNow
                }
            }

            mPreferences.fetcherListener?.doneFetching()
        })

        /**
         * Registration
         */
        mainViewModel.userRegisterErrorData.observe(this, Observer { errorData ->
            Timber.d("User register failed, code: ${errorData?.code}, message: ${errorData?.message}")
            Toast.makeText(this, "User register failed, code: ${errorData?.code}, " +
                    "message: ${errorData?.message}", Toast.LENGTH_LONG).show()
        })

        mainViewModel.userRegisterUiMode.observe(this, Observer { uiMode ->
            mBinding?.waitingView?.setVisibleImmediate(View.GONE)
            mBinding?.faceVerifyResultView?.visibility = View.VISIBLE

            sharedViewModel.registerFinishEvent.postValue(true)

            startSubscriber()

            when (uiMode) {
                Constants.UI_REGISTER_FAILED -> {
                    mBinding?.faceVerifyResultView?.setFailedUI(sharedViewModel, mPreferences)
                    mBinding?.faceVerifyResultView?.unknownLabel = mainViewModel.getRegisterName()
                    mBinding?.faceVerifyResultView?.failedLabel = getString(R.string.registration_fail)
                    mBinding?.faceVerifyResultView?.failedText = "Description description description description description description description"
                }

                Constants.UI_REGISTER_COMPLETE -> {
                    mBinding?.faceVerifyResultView?.setRegisterSuccessUI(sharedViewModel, registerViewModel, mPreferences)
                    mBinding?.faceVerifyResultView?.userImage = DeviceUtils.mFacePngList[0]
                    mBinding?.faceVerifyResultView?.infoText = "Information information information information information"
                    val dateNow = Date()
                    mBinding?.faceVerifyResultView?.clockTime = dateNow
                    mBinding?.faceVerifyResultView?.calendarTime = dateNow
                }
            }

            mPreferences.fetcherListener?.doneFetching()
        })

        mBinding?.faceVerifyResultView?.clockTypeLiveEvent?.observe(this, Observer { clockType ->
            startSubscriber()

            when (clockType) {
                FaceVerifyResultView.CLOCK_TIMEOUT -> {
                    Toast.makeText(this, getString(R.string.clock_timeout), Toast.LENGTH_SHORT).show()

                    startSubscriber()

                    mBinding?.faceVerifyResultView?.visibility = View.GONE
                    backToPreviousPage()

                    mainViewModel.restartSingleMode()
                }
                FaceVerifyResultView.CLOCK_HOLD_RESULT_TIMEOUT -> {
                    startSubscriber()

                    mBinding?.faceVerifyResultView?.visibility = View.GONE
                    backToPreviousPage()

                    mainViewModel.restartSingleMode()
                }
                else -> {
                    // open door
                    when (mPreferences.doorModule) {
                        Constants.BLUETOOTH -> mBtLeManager.openDoorOne()
                        Constants.COM_USB_RELAY -> mUsbRelayManager.openDoor()
                    }

                    // save clock event to database
                    mainViewModel.sendSuccessClockEvent(clockType)
                }
            }
        })

        mBinding?.faceVerifyResultView?.hideFaceResultView?.observe(this, Observer { isHide ->
            if (isHide) {
                mBinding?.faceVerifyResultView?.visibility = View.GONE
                backToPreviousPage()

                mainViewModel.restartSingleMode()
            }
        })

        mainViewModel.deviceLoginData.observe(this, Observer {
            Timber.d("deviceLoginData deviceToken = ${it?.deviceToken.toString()}")
            Timber.d("deviceLoginData locale = ${it?.locale.toString()}")
            Timber.d("deviceLoginData deviceName = ${it?.deviceName.toString()}")
            Timber.d("deviceLoginData modulesModes = ${it?.modulesModes.toString()}")

            // remember device had been initialized
            mPreferences.isDeviceDbExist = true
            startSubscriber()

            // turn on bluetooth is necessary
            turnBluetoothOn()

            //depend on modules
            sharedViewModel.deviceLoginData.value = it
            sharedViewModel.deviceName = it.deviceName

            // connect to websocket
            mWebSocketManager.connect(DeviceUtils.mWsUri, DeviceUtils.WEB_SOCKET_TIME_OUT, this)

            it?.locale?.let { locale ->
                DeviceUtils.setDeviceInfo(baseContext, locale)
            }
            initNavigationMap(it)

            // login success, start automatic clock event sending function
            mainViewModel.startClockEventSendingJob()

            mPreferences.fetcherListener?.doneFetching()
        })
    }

    private fun initNavigationMap(it: DeviceLoginData?) {
        val navHostFragment = container as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.main)
        var label: String? = null

        //decide where page to go
        if (it?.modulesModes?.size ?: 0 > 0) {
            if (it?.modulesModes?.size == 1) {
                //employee or visitor, check the modes

                if (it.modulesModes?.get(0)?.modes?.size ?: 0 > 0) {
                    //set module
                    sharedViewModel.clockModule = it.modulesModes?.get(0)?.module ?: 0

                    if (it.modulesModes?.get(0)?.modes?.size == 1) {
                        when (it.modulesModes?.get(0)?.modes?.get(0)) {
                            SharedViewModel.MODE_SECURITY -> {
                                graph.startDestination = R.id.securityCodeFragment
                                label = SecurityCodeFragment::class.java.simpleName
                            }
                            SharedViewModel.MODE_RFID -> {
                                graph.startDestination = R.id.RFIDFragment
                                label = RFIDFragment::class.java.simpleName
                            }
                            SharedViewModel.MODE_QR_CODE -> {
                                graph.startDestination = R.id.qrCodeFragment
                                label = QrCodeFragment::class.java.simpleName
                            }
                            SharedViewModel.MODE_FACE_ICON -> {
                                //graph.startDestination = R.id.faceIdentificationFragment
                            }
                            SharedViewModel.MODE_FACE_IDENTIFICATION -> {
                                graph.startDestination = R.id.faceIdentificationFragment
                                label = FaceIdentificationFragment::class.java.simpleName
                            }
                        }
                    } else {
                        //go to chooseMode page
                        graph.startDestination = R.id.chooseModeFragment
                        label = ChooseModeFragment::class.java.simpleName
                    }
                } else {
                    //TODO : mode size 0 or null, show error page
                }
            } else {
                //module size > 1, always two modules
                graph.startDestination = R.id.chooseMemberFragment
                label = ChooseMemberFragment::class.java.simpleName
            }
        } else {
            //TODO : module size 0 or null, show error page
            graph.startDestination = R.id.baseFragment
            label = BaseFragment::class.java.simpleName
        }

        navHomeId = graph.startDestination
        navHomeLabel = label
        navHostFragment.navController.graph = graph
    }

    private fun initSetting() {
        initObbManager()
        initFdrManager()
        initWebSocketManager()
        initBtLeManager()
        initUsbRelayManager()
    }

    private fun initObbManager() {
        // OBB file
        mObbManager.initSetting(this)

        if (!mPreferences.isObbLoaded) {
            mBinding?.obbProgressView?.setVisibleWithAnimate(View.VISIBLE)
            mObbManager.start()
            isInitObbManager = true
        } else {
            initSettingFinished()
        }

        mObbManager.downloadStateLiveEvent.observe(this, Observer { state ->
            mBinding?.obbProgressView?.onDownLoadState(state)
        })

        mObbManager.curProgressLiveEvent.observe(this, Observer { progress ->
            mBinding?.obbProgressView?.onProgress(progress)
        })

        mObbManager.curStateLiveEvent.observe(this, Observer { state ->
            mBinding?.obbProgressView?.onState(state)
        })

        mObbManager.mStateController.completeStateResult.observe(this, Observer { result ->
            mPreferences.isObbLoaded = true
            mBinding?.obbProgressView?.onComplete(result)

            SimpleRxTask.afterOnMain(200) {
                Timber.d("ObbManager complete")
                mBinding?.obbProgressView?.setVisibleImmediate(View.GONE)
                initSettingFinished()
            }
        })

        mBinding?.obbProgressView?.progressStateLiveEvent?.observe(this, Observer { state ->
            when (state) {
                ObbProgressView.STATE_PAUSE -> mObbManager.pause()

                ObbProgressView.STATE_RESUME,
                ObbProgressView.STATE_RETRY -> mObbManager.start()
            }
        })
    }

    private fun initFdrManager() {
        // Init FDR manager, must be initialized after the ObbManager
        mFdrManager.initFdr(this, factory)

        mFdrManager.fdrMainEvent.observe(this, Observer { status ->
            startSubscriber()

            when (status) {
                FdrManager.STATUS_IDENTIFYING_FACE,
                FdrManager.STATUS_FACE_FORWARD_CAMERA -> {
                    //Timber.d("Observe fdrManager status: STATUS_IDENTIFYING_FACE | STATUS_FACE_FORWARD_CAMERA")
                }

                FdrManager.STATUS_GET_FACE_FAILED -> {
                    Timber.d("Observe fdrManager status: STATUS_GET_FACE_FAILED")
                    mainViewModel.onGetFaceFailed()
                }

                FdrManager.STATUS_GET_FACE_SUCCESS -> {
                    Timber.d("Observe fdrManager status: STATUS_GET_FACE_SUCCESS")
                    mainViewModel.onGetFaceSuccess()
                }

                FdrManager.STATUS_GET_FACE_TIMEOUT -> {
                    Timber.d("Observe fdrManager status: STATUS_GET_FACE_TIMEOUT")

                    // show timeout toast and popup to previous page
                    Toast.makeText(this, getString(R.string.face_verify_time_expire), Toast.LENGTH_LONG).show()
                    backToPreviousPage()
                }
            }
        })
    }

    private fun initWebSocketManager() {
        mWebSocketManager.webSocketStateEvent.observe(this, Observer { state ->
            when (state) {
                WebSocket.STATE_WEB_SOCKET_CONNECT -> {
                    Timber.d("WebSocket state: STATE_WEB_SOCKET_CONNECT")
                    mBinding?.socketImage?.setImageResource(R.mipmap.ic_connected)
                }

                WebSocket.STATE_WEB_SOCKET_DISCONNECT -> {
                    Timber.d("WebSocket state: STATE_WEB_SOCKET_DISCONNECT")
                    mBinding?.socketImage?.setImageResource(R.mipmap.ic_disconnected)

                    if (AttendanceApp.isActivityVisible()) {
                        Timber.d("Do web socket reconnect")
                        mWebSocketManager.reconnect(DeviceUtils.mWsUri, DeviceUtils.WEB_SOCKET_TIME_OUT, this)
                    }
                }

                WebSocket.STATE_CHECK_WEB_SOCKET_ALIVE -> {
                    Timber.d("WebSocket state: STATE_CHECK_WEB_SOCKET_ALIVE")
                    mWebSocketManager.mWebSocket?.sendCheckAliveEvent()

                    if (AttendanceApp.isActivityVisible()) {
                        Timber.d("Do web socket reconnect")
                        mWebSocketManager.reconnect(DeviceUtils.mWsUri, DeviceUtils.WEB_SOCKET_TIME_OUT, this)
                    }
                }
            }
        })

        mWebSocketManager.webSocketSyncEvent.observe(this, Observer { syncData ->
            if (!syncData.contains("map_session_id") && !syncData.contains("TestConnection")) {
                Toast.makeText(this, "On receive web socket event: $syncData", Toast.LENGTH_LONG).show()
            }

            when (syncData) {
                WebSocket.PUSH_MESSAGE_SYNC_EMPLOYEE -> {
                    // Update employees + visitors + identities
                    mainViewModel.deleteAllAcceptance()
                    mainViewModel.getDeviceEmployees(mPreferences.tabletToken)
                    mainViewModel.getDeviceVisitors(mPreferences.tabletToken)
                    mainViewModel.getDeviceIdentities(mPreferences.tabletToken)
                }

                WebSocket.PUSH_MESSAGE_SYNC_VISITOR -> {
                    // Update employees + visitors + identities
                    mainViewModel.deleteAllAcceptance()
                    mainViewModel.getDeviceEmployees(mPreferences.tabletToken)
                    mainViewModel.getDeviceVisitors(mPreferences.tabletToken)
                    mainViewModel.getDeviceIdentities(mPreferences.tabletToken)
                }

                WebSocket.PUSH_MESSAGE_SYNC_MARQUEE -> {
                    // Update marquee
                    mainViewModel.getDeviceMarquees(mPreferences.tabletToken)
                }

                WebSocket.PUSH_MESSAGE_SYNC_VIDEO -> {
                    // Close screen saver first

                    // Update screen saver
                    mainViewModel.getDeviceVideos(mPreferences.tabletToken, true)
                }

                WebSocket.PUSH_MESSAGE_SYNC_ALL -> {
                    // Update employees + visitors + identities + marquee + screen saver
                    mainViewModel.deleteAllAcceptance()
                    mainViewModel.getDeviceEmployees(mPreferences.tabletToken)
                    mainViewModel.getDeviceVisitors(mPreferences.tabletToken)
                    mainViewModel.getDeviceIdentities(mPreferences.tabletToken)
                    mainViewModel.getDeviceMarquees(mPreferences.tabletToken)
                    mainViewModel.getDeviceVideos(mPreferences.tabletToken, true)
                }

                WebSocket.PUSH_MESSAGE_TEST_CONNECTION -> {
                    mWebSocketManager.mWebSocket?.sendCheckAliveEvent()
                }

                WebSocket.PUSH_MESSAGE_RESTART -> {
                    mPreferences.savePreferences()

                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.putExtra("crash", true)

                    val restartIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
                    val mgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, restartIntent)     // restart after 1 second

                    android.os.Process.killProcess(android.os.Process.myPid())
                    finish()
                    exitProcess(0)
                }
            }
        })
    }

    private fun initBtLeManager() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        Timber.d("bluetoothAdapter.isEnabled() = %s", bluetoothAdapter.isEnabled)
        if (bluetoothAdapter.isEnabled) {
            mBtLeManager.initSetting(this)
        }
    }

    private fun initUsbRelayManager() {
        mUsbRelayManager.initUsbRelayManager(this)
        mUsbRelayManager.start()
    }

    private fun initSettingFinished() {
        if (mPreferences.serverIp.isEmpty() || mPreferences.tabletToken.isEmpty()) {
            Toast.makeText(this, getString(R.string.empty_login_info), Toast.LENGTH_LONG).show()
        } else {
            mInterceptor.setHost("http://" + mPreferences.serverIp)
            startLogin()
        }
    }

    private fun startLogin() {
        Timber.d("startLogin()")

        //Test Login Api
        val deviceToken = mPreferences.tabletToken
        val deviceType = "Android"
        val deviceIp = "192.168.3.66"

        when {
            mNetworkChecker.isNetworkAvailable() -> {
                mainViewModel.deviceInit(deviceToken, deviceType, deviceIp)
                mainViewModel.getDeviceMarquees(deviceToken)
                mainViewModel.getDeviceVideos(deviceToken)
            }

            mPreferences.isDeviceDbExist -> {
                // Get Db Data
                mainViewModel.deviceInitializedFromDb(deviceToken)
                mainViewModel.getDeviceMarqueesFromDB(deviceToken)
                mainViewModel.getDeviceVideosFromDB(deviceToken)
            }

            else -> {
                Toast.makeText(this, "Device network is unavailable and database is empty", Toast.LENGTH_LONG).show()
                navigateToBaseFragment()

                mBinding?.errorHintText?.visibility = View.VISIBLE
            }
        }
    }

    fun navigateToBaseFragment() {
        val navHostFragment = container as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.main)
        graph.startDestination = R.id.baseFragment
        navHostFragment.navController.graph = graph
    }

    private fun backToPreviousPage() {
        if (!sharedViewModel.isSingleModuleMode()) {
            navController.popBackStack()
        }
    }

    override fun supportFragmentInjector() = dispatchingAndroidInjector
}
