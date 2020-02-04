package com.gorilla.attendance.ui.setting

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.navigation.Navigation
import com.gorilla.attendance.R
import com.gorilla.attendance.databinding.SettingFragmentBinding
import com.gorilla.attendance.di.Injectable
import com.gorilla.attendance.ui.common.BaseFragment
import com.gorilla.attendance.ui.main.MainActivity
import com.jakewharton.rxbinding.view.RxView
import timber.log.Timber
import java.util.concurrent.TimeUnit
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.zxing.ResultPoint
import com.google.zxing.client.android.Intents
import com.gorilla.attendance.data.model.MyBluetoothDevice
import com.gorilla.attendance.utils.BtLeManager
import com.gorilla.attendance.utils.Constants
import com.gorilla.attendance.utils.DeviceUtils
import com.jakewharton.rxbinding.widget.RxTextView
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import javax.inject.Inject


class SettingFragment : BaseFragment(), Injectable {

    companion object {
        const val STATE_VERIFY_SETTING_ACCOUNT = 0
        const val STATE_MODIFY_SETTING_CONFIG = 1
    }

    @Inject
    lateinit var mBtLeManager: BtLeManager

    private var mBinding: SettingFragmentBinding? = null

    private lateinit var settingViewModel: SettingViewModel

    private var optionSubscribe: Disposable? = null
    private var deviceListSubscribe: Disposable? = null

    private var preBtPassword = ""

    private var editSettingState = STATE_VERIFY_SETTING_ACCOUNT

    var preToolbarVisibility = View.VISIBLE

    private var enableDeviceCount = 0
    private var enableDeviceTime = 0L

    /**
     * Parameters which will cause client to do re-login
     */
    private lateinit var oldServerIp: String
    private lateinit var oldWebSocketIp: String
    private lateinit var oldDeviceToken: String
    private var oldApplicationMode: Int = 0

    private var webCamType: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate()")
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.d("onCreateView()")
        mBinding = SettingFragmentBinding.inflate(inflater, container, false)

        return mBinding?.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Timber.d("onActivityCreated()")

        settingViewModel = ViewModelProviders.of(this, factory).get(SettingViewModel::class.java)

        // observe setting account verified result
        settingViewModel.verifyAccountEvent.observe(this, Observer { isValid ->
            isValid?.let {
                if (it) {
                    editSettingState = STATE_MODIFY_SETTING_CONFIG

                    mBinding?.btnLeft?.visibility = View.VISIBLE
                    mBinding?.btnRight?.visibility = View.VISIBLE
                    mBinding?.btnRight?.text = getString(R.string.save_and_apply)

                    mBinding?.authLayout?.visibility = View.GONE
                    mBinding?.settingOptionsView?.visibility = View.VISIBLE
                    mBinding?.connectionLayout?.visibility = View.VISIBLE
                } else {
                    // show error UI
                    showErrorAccountUI()
                }
            }
        })

        initUI()

        initViewModelObservers()

        mBinding?.viewModel = settingViewModel

        mFdrManager.stopFdr()
    }

    override fun onStart() {
        super.onStart()
        Timber.d("onStart sharedViewModel.clockModule = ${sharedViewModel.clockModule}")
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume()")

        sharedViewModel.changeTitleEvent.postValue(getString(R.string.setting_title))
    }

    override fun onPause() {
        super.onPause()
        Timber.d("onPause()")

        mBinding?.barcodeScanner?.pauseAndWait()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy()")
        optionSubscribe?.dispose()
        deviceListSubscribe?.dispose()

        // restore bluetooth password
        mPreferences.bluetoothPassword = preBtPassword

        if (preToolbarVisibility == View.INVISIBLE) {
            (activity as MainActivity).setToolbarVisible(false)
        }
    }

    private fun initUI() {
        preToolbarVisibility = (activity as MainActivity).getToolbarVisibility()
        (activity as MainActivity).setToolbarVisible(true, isForce = true)

        mBinding?.btnLeft?.let {
            RxView.clicks(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .subscribe {
                    // cancel
                    Navigation.findNavController(mBinding?.root!!).popBackStack()
                }
        }

        mBinding?.btnRight?.text = getString(R.string.login)
        mBinding?.btnRight?.let {
            RxView.clicks(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(rx.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe {
                    when (editSettingState) {
                        STATE_VERIFY_SETTING_ACCOUNT -> {
                            // Validate setting account
                            settingViewModel.verifyAccount(
                                mBinding?.settingAccountEditText?.text.toString(),
                                mBinding?.settingPasswordEditText?.text.toString()
                            )
                        }

                        STATE_MODIFY_SETTING_CONFIG -> {
                            // check setting is valid or not
                            if (!checkNewSetting()) {
                                return@subscribe
                            }

                            saveSetting()

                            //if (isNeedInitFdr()) {
                                (activity as MainActivity).initFdr()
                            //}

                            (activity as MainActivity).startSubscriber()

                            if (isNeedLogin()) {
                                // save and apply
                                (activity as MainActivity).applyNewSetting(isNeedReconnectWebSocket())
                            } else {
                                (activity as MainActivity).navigateToHome()
                            }
                        }
                    }
                    hideSoftwareKeyboard()
                }
        }

        mBinding?.tabletTokenQrBtn?.let {
            RxView.clicks(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(rx.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe {
                    Timber.d("before barcodeScanner isEnabled: ${mBinding?.barcodeScanner?.isEnabled}")
                    Timber.d("before barcodeScanner isActivated: ${mBinding?.barcodeScanner?.isActivated}")
                    startQrCodeScan()
                    Timber.d("after barcodeScanner isEnabled: ${mBinding?.barcodeScanner?.isEnabled}")
                    Timber.d("after barcodeScanner isActivated: ${mBinding?.barcodeScanner?.isActivated}")
                }
        }

        mBinding?.btRefreshBtn?.let {
            RxView.clicks(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(rx.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe {
                    val deviceListAdapter = mBinding?.deviceList?.adapter as DeviceListAdapter
                    deviceListAdapter.devices.clear()
                    if (!DeviceUtils.isBluetoothDisconnected) {
                        deviceListAdapter.devices.add(MyBluetoothDevice(
                            mPreferences.bluetoothDeviceName, mPreferences.bluetoothAddress, 0))
                    }
                    deviceListAdapter.notifyDataSetChanged()
                    mBtLeManager.scanLeDevice(true)
                }
        }

        mBinding?.btOpenBtn?.let {
            RxView.clicks(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .subscribe {
                    mBtLeManager.openDoorOne()
                }
        }

        mBinding?.btCloseBtn?.let {
            RxView.clicks(it)
                .debounce(300, TimeUnit.MILLISECONDS)
                .subscribe {
                    mBtLeManager.closeDoorOne()
                }
        }

        mBinding?.closePasswordText?.let {
            RxTextView.textChanges(it)
                .subscribe { text ->
                    if (!text.isNullOrEmpty()) {
                        mPreferences.bluetoothPassword = text.toString()
                    }
                }
        }

        mBinding?.settingAccountEditText?.let {
            RxTextView.textChanges(it)
                .observeOn(rx.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe {
                    hideErrorAccountUI()
                }
        }

        mBinding?.settingPasswordEditText?.let {
            RxTextView.textChanges(it)
                .observeOn(rx.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe {
                    hideErrorAccountUI()
                }
        }

        mBinding?.settingPasswordEditText?.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                mBinding?.btnRight?.performClick()
            }

            false
        }

        // init barcode scan view
        val intent = Intent()
        intent.putExtra(Intents.Scan.CAMERA_ID, 0)
        val cameraId = intent.extras?.getInt(Intents.Scan.CAMERA_ID)
        Timber.d("Init scan view with camera id = $cameraId")
        mBinding?.barcodeScanner?.initializeFromIntent(intent)
        mBinding?.barcodeScanner?.setStatusText("")

        initRecyclerView()

        initSpinnerView()

        initSwitchView()

        // must call at the end of initUI() function
        initSetting()

        mBinding?.serverIpLabel?.let {
            RxView.clicks(it)
                .observeOn(rx.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe {
                    triggerEnableDevice()
                }
        }

        mBinding?.tabletTokenLabel?.let {
            RxView.clicks(it)
                .observeOn(rx.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe {
                    triggerEnableDevice()
                }
        }

        if (mPreferences.isLoginFinish) {
            mBinding?.serverIpEditText?.isEnabled = false
            mBinding?.serverIpEditText?.alpha = 0.5f

            mBinding?.tabletTokenEditText?.isEnabled = false
            mBinding?.tabletTokenEditText?.alpha = 0.5f

            mBinding?.tabletTokenQrBtn?.isEnabled = false
            mBinding?.tabletTokenQrBtn?.alpha = 0.5f
        }
    }

    private fun triggerEnableDevice() {
        if (enableDeviceCount >= DeviceUtils.ENTER_SETTING_CLICK_NUM
            || (System.currentTimeMillis() - enableDeviceTime) > DeviceUtils.ENTER_SETTING_INTERVAL_TIME) {
            enableDeviceCount = 0
        }

        if (enableDeviceCount == 0) {
            enableDeviceTime = System.currentTimeMillis()
        }

        enableDeviceCount++

        if (enableDeviceCount >= DeviceUtils.ENTER_SETTING_CLICK_NUM) {
            enableDeviceCount = 0

            mBinding?.serverIpEditText?.isEnabled = true
            mBinding?.serverIpEditText?.alpha = 1.0f

            mBinding?.tabletTokenEditText?.isEnabled = true
            mBinding?.tabletTokenEditText?.alpha = 1.0f

            mBinding?.tabletTokenQrBtn?.isEnabled = true
            mBinding?.tabletTokenQrBtn?.alpha = 1.0f
        }
    }

    private fun isNeedLogin(): Boolean {
        return (oldServerIp != mPreferences.serverIp
                || oldWebSocketIp != mPreferences.webSocketIp
                || oldDeviceToken != mPreferences.tabletToken
                || oldApplicationMode != mPreferences.applicationMode)
    }

    private fun isNeedReconnectWebSocket(): Boolean {
        return (oldWebSocketIp != mPreferences.webSocketIp
                || oldDeviceToken != mPreferences.tabletToken)
    }

    private fun initRecyclerView() {
        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = RecyclerView.VERTICAL
        val adapter = SettingAdapter(context!!)
        val list = (resources.getStringArray(R.array.setting_array)).toCollection(ArrayList())
        adapter.items = list
        optionSubscribe = adapter.clickEvent
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe { clickName ->
                Timber.d("Click options: $clickName")
                mBinding?.connectionLayout?.visibility = View.GONE
                mBinding?.functionLayout?.visibility = View.GONE
                mBinding?.linkedLayout?.visibility = View.GONE
                mBinding?.linkedLayout2?.visibility = View.GONE
                mBinding?.updateCycleLayout?.visibility = View.GONE
                mBinding?.bluetoothLayout?.visibility = View.GONE

                when (clickName) {
                    list[0] -> {
                        mBinding?.connectionLayout?.visibility = View.VISIBLE
                    }

                    list[1] -> {
                        mBinding?.functionLayout?.visibility = View.VISIBLE
                    }

                    list[2] -> {
                        mBinding?.linkedLayout?.visibility = View.VISIBLE
                    }

                    list[3] -> {
                        mBinding?.linkedLayout2?.visibility = View.VISIBLE
                    }

                    list[4] -> {
                        mBinding?.updateCycleLayout?.visibility = View.VISIBLE
                    }

                    list[5] -> {
                        mBinding?.bluetoothLayout?.visibility = View.VISIBLE
                    }

                    else -> {}
                }
            }
        mBinding?.settingOptionsView?.adapter = adapter
        mBinding?.settingOptionsView?.layoutManager = layoutManager
        (mBinding?.settingOptionsView?.adapter)?.notifyDataSetChanged()


        // about bluetooth devices
        val deviceListManager = LinearLayoutManager(context)
        deviceListManager.orientation = RecyclerView.VERTICAL
        val deviceListAdapter = DeviceListAdapter(context!!)

        deviceListSubscribe = deviceListAdapter.clickEvent
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe { device ->
                Timber.d("Click device name: ${device.deviceName}")

                // TODO show bluetooth connecting dialog

                mBtLeManager.mBluetoothLeService?.let {
                    Timber.d("do bluetooth disconnect")
                    mBtLeManager.bluetoothDoorDisconnect()
                }

                if (mBtLeManager.mScanning) {
                    mBtLeManager.scanLeDevice(false)
                }

                mPreferences.bluetoothDeviceName = device.deviceName!!

                mBtLeManager.bluetoothDoorConnect(device.address)
                deviceListAdapter.curAddress = device.address
            }

        mBinding?.deviceList?.adapter = deviceListAdapter
        mBinding?.deviceList?.layoutManager = deviceListManager

        mBtLeManager.scanResultEvent.observe(this, Observer { device ->
            // add available device
            device?.let {
                deviceListAdapter.addDevice(it)
                deviceListAdapter.notifyDataSetChanged()
            }
        })

        deviceListAdapter.devices.clear()
        if (!DeviceUtils.isBluetoothDisconnected) {
            deviceListAdapter.devices.add(MyBluetoothDevice(
                mPreferences.bluetoothDeviceName, mPreferences.bluetoothAddress, 0))
        }
        deviceListAdapter.curAddress = mPreferences.bluetoothAddress
        mBtLeManager.scanLeDevice(true)
    }

    private fun initSpinnerView() {
        val modeAdapter = SettingSpinnerAdapter(context!!, resources.getStringArray(R.array.app_mode_array))
        mBinding?.appModeSpinner?.adapter = modeAdapter
        mBinding?.appModeSpinner?.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                (mBinding?.appModeSpinner?.adapter as SettingSpinnerAdapter).curPosition = position
                (mBinding?.appModeSpinner?.adapter as SettingSpinnerAdapter).notifyDataSetChanged()
            }
        }

        val checkAdapter = SettingSpinnerAdapter(context!!, resources.getStringArray(R.array.check_in_out_array))
        mBinding?.checkInOutSpinner?.adapter = checkAdapter
        mBinding?.checkInOutSpinner?.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                (mBinding?.checkInOutSpinner?.adapter as SettingSpinnerAdapter).curPosition = position
                (mBinding?.checkInOutSpinner?.adapter as SettingSpinnerAdapter).notifyDataSetChanged()
            }
        }

        val webcamAdapter = SettingSpinnerAdapter(context!!, resources.getStringArray(R.array.webcam_array))
        mBinding?.webcamSpinner?.adapter = webcamAdapter
        mBinding?.webcamSpinner?.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                (mBinding?.webcamSpinner?.adapter as SettingSpinnerAdapter).curPosition = position
                (mBinding?.webcamSpinner?.adapter as SettingSpinnerAdapter).notifyDataSetChanged()

                when (position) {
                    Constants.ANDROID_BUILD_IN_LENS -> {
                        // hide RTSP UI
                        mBinding?.webcamGroup?.visibility = View.GONE
                    }

                    Constants.RTSP_WEB_CAM -> {
                        // show RTSP setting UI
                        mBinding?.webcamGroup?.visibility = View.VISIBLE
                    }
                }
            }
        }

        val doorAdapter = SettingSpinnerAdapter(context!!, resources.getStringArray(R.array.door_module_array))
        mBinding?.doorModuleSpinner?.adapter = doorAdapter
        mBinding?.doorModuleSpinner?.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    // Show bluetooth setting
                    (mBinding?.settingOptionsView?.adapter as SettingAdapter).showBluetooth()
                    (mBinding?.settingOptionsView?.adapter)?.notifyDataSetChanged()
                } else {
                    // Hide bluetooth setting
                    (mBinding?.settingOptionsView?.adapter as SettingAdapter).hideBluetooth()
                    (mBinding?.settingOptionsView?.adapter)?.notifyDataSetChanged()
                }

                (mBinding?.doorModuleSpinner?.adapter as SettingSpinnerAdapter).curPosition = position
                (mBinding?.doorModuleSpinner?.adapter as SettingSpinnerAdapter).notifyDataSetChanged()
            }
        }

        if (mPreferences.doorModule == Constants.BLUETOOTH) {
            // Show bluetooth setting
            (mBinding?.settingOptionsView?.adapter as SettingAdapter).showBluetooth()
            (mBinding?.settingOptionsView?.adapter)?.notifyDataSetChanged()
        } else {
            // Hide bluetooth setting
            (mBinding?.settingOptionsView?.adapter as SettingAdapter).hideBluetooth()
            (mBinding?.settingOptionsView?.adapter)?.notifyDataSetChanged()
        }
    }

    private fun initSwitchView() {
        mBinding?.humanSwitch?.setOnCheckedChangeListener { _, isChecked ->
            mBinding?.isHumanChecked = isChecked
        }

        mBinding?.fdrSwitch?.setOnCheckedChangeListener { _, isChecked ->
            mBinding?.isFdrOnlineChecked = isChecked
        }
    }

    /**
     * Init setting of UI, ex: value of EditTexts, select position of Spinners
     */
    private fun initSetting() {
        oldServerIp = mPreferences.serverIp
        oldWebSocketIp = mPreferences.webSocketIp
        oldDeviceToken = mPreferences.tabletToken
        oldApplicationMode = mPreferences.applicationMode
        webCamType = mPreferences.webcamType

        // connection setting
        mBinding?.serverIpEditText?.setText(mPreferences.serverIp)
        mBinding?.webSocketIpEditText?.setText(mPreferences.webSocketIp)
        mBinding?.ftpAccountEditText?.setText(mPreferences.ftpAccount)
        mBinding?.ftpPasswordEditText?.setText(mPreferences.ftpPassword)
        //mBinding?.tabletNameEditText?.setText(mPreferences.tabletName)
        mBinding?.tabletNameEditText?.setText(sharedViewModel.deviceName)
        mBinding?.tabletTokenEditText?.setText(mPreferences.tabletToken)

        // function setting
        mBinding?.idleTimeEditText?.setText(mPreferences.idleTime.toString())
        mBinding?.holdResultEditText?.setText(mPreferences.holdResultTime.toString())
        mBinding?.faceVerifyTimeEditText?.setText(mPreferences.faceVerifyTime.toString())
        mBinding?.appModeSpinner?.setSelection(mPreferences.applicationMode)
        mBinding?.checkInOutSpinner?.setSelection(mPreferences.checkMode)
        mBinding?.isHumanChecked = mPreferences.isEnableHumanDetection
        mBinding?.isFdrOnlineChecked = mPreferences.isFdrOnlineMode
        mBinding?.isScreenSaverEnable = mPreferences.isScreenSaverEnable

        // linked setting
        mBinding?.webcamSpinner?.setSelection(mPreferences.webcamType)
        mBinding?.webcamUrlEditText?.setText(mPreferences.webcamUrl)
        mBinding?.webcamUsernameEditText?.setText(mPreferences.webcamUsername)
        mBinding?.webcamPasswordEditText?.setText(mPreferences.webcamPassword)
        mBinding?.doorModuleSpinner?.setSelection(mPreferences.doorModule)

        // update cycle setting
        mBinding?.refreshUserEditText?.setText(mPreferences.updateUserTime.toString())
        mBinding?.refreshRecordsEditText?.setText(mPreferences.updateRecordTime.toString())
        mBinding?.refreshOthersEditText?.setText(mPreferences.updateOtherTime.toString())
        mBinding?.clearCycleEditText?.setText(mPreferences.clearDataTime.toString())

        // bluetooth setting
        mBinding?.closeDoorEditText?.setText(mPreferences.closeDoorTimeout.toString())
        mBinding?.closePasswordText?.setText(mPreferences.bluetoothPassword)
        preBtPassword = mPreferences.bluetoothPassword
    }

    /**
     * Check new setting, if setting is invalid, show hint dialog to user
     */
    private fun checkNewSetting(): Boolean {
        if (mBinding?.serverIpEditText?.text.toString().isEmpty()) {
            Toast.makeText(context!!, getString(R.string.invalid_server_ip), Toast.LENGTH_LONG).show()
            return false
        }

        if (mBinding?.webSocketIpEditText?.text.toString().isEmpty()) {
            Toast.makeText(context!!, getString(R.string.invalid_web_socket_ip), Toast.LENGTH_LONG).show()
            return false
        }

        if (mBinding?.ftpAccountEditText?.text.toString().isEmpty()) {
            Toast.makeText(context!!, getString(R.string.invalid_ftp_account), Toast.LENGTH_LONG).show()
            return false
        }

        if (mBinding?.ftpPasswordEditText?.text.toString().isEmpty()) {
            Toast.makeText(context!!, getString(R.string.invalid_ftp_password), Toast.LENGTH_LONG).show()
            return false
        }

        if (mBinding?.tabletTokenEditText?.text.toString().isEmpty()) {
            Toast.makeText(context!!, getString(R.string.invalid_device_token), Toast.LENGTH_LONG).show()
            return false
        }

        if (mBinding?.closePasswordText?.text.toString().length != 8) {
            Toast.makeText(context!!, getString(R.string.invalid_bluetooth_passowrd), Toast.LENGTH_LONG).show()
            return false
        }

        // If use RTSP camera, need to check setting
        if (mBinding?.webcamSpinner?.selectedItemPosition != 0) {
            if (mBinding?.webcamUrlEditText?.text.isNullOrEmpty()
                || mBinding?.webcamUsernameEditText?.text.isNullOrEmpty()
                || mBinding?.webcamPasswordEditText?.text.isNullOrEmpty()) {
                Toast.makeText(context!!, getString(R.string.invalid_rtsp_setting), Toast.LENGTH_LONG).show()
                return false
            }
        }

        return true
    }

    /**
     * Apply user changes into preferences
     */
    private fun saveSetting() {
        // connection setting
        mPreferences.serverIp = mBinding?.serverIpEditText?.text.toString()
        mPreferences.webSocketIp = mBinding?.webSocketIpEditText?.text.toString()
        mPreferences.ftpAccount = mBinding?.ftpAccountEditText?.text.toString()
        mPreferences.ftpPassword = mBinding?.ftpPasswordEditText?.text.toString()
        mPreferences.tabletName = mBinding?.tabletNameEditText?.text.toString()
        mPreferences.tabletToken = mBinding?.tabletTokenEditText?.text.toString()

        // function setting
        mPreferences.idleTime = mBinding?.idleTimeEditText?.text.toString().toLong()
        mPreferences.holdResultTime = mBinding?.holdResultEditText?.text.toString().toLong()
        mPreferences.faceVerifyTime = mBinding?.faceVerifyTimeEditText?.text.toString().toLong()
        mPreferences.applicationMode = mBinding?.appModeSpinner?.selectedItemPosition ?: 0
        mPreferences.checkMode = mBinding?.checkInOutSpinner?.selectedItemPosition ?: 0
        mPreferences.isEnableHumanDetection = mBinding?.isHumanChecked ?: false
        mPreferences.isFdrOnlineMode = mBinding?.isFdrOnlineChecked ?: true
        mPreferences.isScreenSaverEnable = mBinding?.isScreenSaverEnable ?: true

        // linked setting
        mPreferences.webcamType = mBinding?.webcamSpinner?.selectedItemPosition ?: 0
        mPreferences.webcamUrl = mBinding?.webcamUrlEditText?.text.toString()
        mPreferences.webcamUsername = mBinding?.webcamUsernameEditText?.text.toString()
        mPreferences.webcamPassword = mBinding?.webcamPasswordEditText?.text.toString()
        mPreferences.doorModule = mBinding?.doorModuleSpinner?.selectedItemPosition ?: 0

        // update cycle setting
        mPreferences.updateUserTime = mBinding?.refreshUserEditText?.text.toString().toInt()
        mPreferences.updateRecordTime = mBinding?.refreshRecordsEditText?.text.toString().toInt()
        mPreferences.updateOtherTime = mBinding?.refreshOthersEditText?.text.toString().toInt()
        mPreferences.clearDataTime = mBinding?.clearCycleEditText?.text.toString().toInt()

        // bluetooth setting
        mPreferences.closeDoorTimeout = mBinding?.closeDoorEditText?.text.toString().toLong()
        mPreferences.bluetoothPassword = mBinding?.closePasswordText?.text.toString()
        preBtPassword = mPreferences.bluetoothPassword

        // set values into preferences
        mPreferences.savePreferences()
    }

    private fun startQrCodeScan() {
        mBinding?.barcodeScanner?.visibility = View.VISIBLE
        mBinding?.barcodeScanner?.resume()
        mBinding?.barcodeScanner?.decodeSingle(mScanCallback)
    }

    private fun showErrorAccountUI() {
        mBinding?.errorHintText?.visibility = View.VISIBLE
        mBinding?.settingAccountEditText?.setBackgroundDrawable(
            ContextCompat.getDrawable(context!!, R.drawable.rounded_border_error))
        mBinding?.settingPasswordEditText?.setBackgroundDrawable(
            ContextCompat.getDrawable(context!!, R.drawable.rounded_border_error))
    }

    private fun hideErrorAccountUI() {
        mBinding?.errorHintText?.visibility = View.GONE
        mBinding?.settingAccountEditText?.setBackgroundDrawable(
            ContextCompat.getDrawable(context!!, R.drawable.rounded_border))
        mBinding?.settingPasswordEditText?.setBackgroundDrawable(
            ContextCompat.getDrawable(context!!, R.drawable.rounded_border))
    }

    private val mScanCallback = object: BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            if (result.text == null) {
                return
            }

//            mBeepManager.playBeepSound()

            Timber.d("mScanCallback result = ${result.text}")

            val code = result.text
            mBinding?.tabletTokenEditText?.setText(code)

            mBinding?.barcodeScanner?.visibility = View.GONE
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
    }

    private fun initViewModelObservers() {

    }
}