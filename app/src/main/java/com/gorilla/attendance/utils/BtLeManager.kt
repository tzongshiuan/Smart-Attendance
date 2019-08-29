package com.gorilla.attendance.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.os.Handler
import android.os.IBinder
import com.gorilla.attendance.data.model.MyBluetoothDevice
import com.gorilla.attendance.service.BluetoothLeService
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/7/29
 * Description: Bluetooth Low Energy Manager
 */
class BtLeManager @Inject constructor(private val mPreferences: AppPreferences) {

    companion object {
        const val SCAN_PERIOD = 10000L  // 10s
    }

    private var mActivity: Activity? = null

    private var mIsBluetoothServiceBind = false

    private var mBluetoothService: IBinder? = null

    var mBluetoothLeService: BluetoothLeService? = null
    private var mIsBTConnected = false

    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothLeScanner: BluetoothLeScanner? = null

    private var mDoorOneTimer: Timer? = null
    private var mDoorTwoTimer: Timer? = null

    var mScanning: Boolean = false

    val scanResultEvent = SingleLiveEvent<MyBluetoothDevice>()

    private val handler = Handler()
    private val checkBtRunnable = object: Runnable {
        override fun run() {
            if (DeviceUtils.isBluetoothDisconnected) {
                reconnect()
            }

            handler.postDelayed(this, DeviceUtils.BLUETOOTH_CHECK_INTERVAL_TIME)
        }
    }

    private val mBluetoothServiceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Timber.d("onServiceConnected()")

            if (service == null) {
                Timber.d("[mBluetoothServiceConnection] service is null")
                return
            }

            mBluetoothService = service
            mIsBluetoothServiceBind = true

            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            Timber.d("DeviceUtils.mBluetoothLeService, bluetooth door address = ${mPreferences.bluetoothAddress}")
            mBluetoothLeService?.connect(mPreferences.bluetoothAddress)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Timber.d("onServiceDisconnected()")
            
            // If disconnected, try to connect again
            mBluetoothService?.let {
                mBluetoothLeService = (mBluetoothService as BluetoothLeService.LocalBinder).service
                Timber.d("DeviceUtils.mBluetoothLeService, bluetooth door address = ${mPreferences.bluetoothAddress}")
                mBluetoothLeService?.connect(mPreferences.bluetoothAddress)
            }
        }
    }

    private val mGattUpdateReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            if (action != BluetoothLeService.ACTION_DATA_AVAILABLE)
                Timber.d("action = $action")

            when (action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> Timber.d("Only gatt, just wait")
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    mIsBTConnected = false

                    //connect + disconnect, to reconnect
                    //                if (getSupportFragmentManager().findFragmentByTag(ConfigureSettingBluetoothFragment.TAG) != null) {
                    //                    val configureSettingBluetoothFragment =
                    //                        getSupportFragmentManager().findFragmentByTag(ConfigureSettingBluetoothFragment.TAG) as ConfigureSettingBluetoothFragment
                    //                    configureSettingBluetoothFragment!!.setBluetoothState(getString(R.string.txt_bluetooth_state_reconnect))

                    Timber.d("Do disconnect + connect")
                    bluetoothDoorDisconnect()
                    bluetoothDoorConnect(null)

                    //                    Toast.makeText(
                    //                        getApplicationContext(),
                    //                        getString(R.string.txt_bluetooth_state_reconnect),
                    //                        Toast.LENGTH_SHORT
                    //                    ).show()
                    //                }
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> mIsBTConnected = true

                //                if (getSupportFragmentManager().findFragmentByTag(ConfigureSettingBluetoothFragment.TAG) != null) {
                //                    val configureSettingBluetoothFragment =
                //                        getSupportFragmentManager().findFragmentByTag(ConfigureSettingBluetoothFragment.TAG) as ConfigureSettingBluetoothFragment
                //                    configureSettingBluetoothFragment!!.setBluetoothState(getString(R.string.txt_bluetooth_state_success))
                //                }
                //
                //                Toast.makeText(
                //                    getApplicationContext(),
                //                    getString(R.string.txt_bluetooth_state_success),
                //                    Toast.LENGTH_SHORT
                //                ).show()
                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    //Timber.d("Receive DATA")
                    val data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA)
                    //                myTV.setText("蓝牙设备连接成功");
                    //                byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    //                String returnMsg = bytesToHexString(data);
                    //                if (returnMsg != null) {
                    //                    System.out.println("传输过来的是"+returnMsg.substring(5, 6));
                    //                    if ("E".equals(returnMsg.substring(5, 6))) {
                    //                        Toast.makeText(getApplicationContext(), "密码错误", Toast.LENGTH_SHORT).show();
                    //                    }
                    //                }
                    if (data != null) {
                    }
                }
                BluetoothLeService.ACTION_GATT_FAIL -> {
                    //connect fail
                    Timber.d("Connect bluetooth failed")
                    mIsBTConnected = false
//                    if (getSupportFragmentManager().findFragmentByTag(ConfigureSettingBluetoothFragment.TAG) != null) {
//                        val configureSettingBluetoothFragment =
//                            getSupportFragmentManager().findFragmentByTag(ConfigureSettingBluetoothFragment.TAG) as ConfigureSettingBluetoothFragment
//                        configureSettingBluetoothFragment!!.setBluetoothState(getString(R.string.txt_bluetooth_state_fail_choose_again))
//                    }
//
//                    Toast.makeText(
//                        getApplicationContext(),
//                        getString(R.string.txt_bluetooth_state_fail_choose_again),
//                        Toast.LENGTH_SHORT
//                    ).show()
                }
            }
        }
    }

    @SuppressLint("WrongConstant")
    fun initSetting(activity: Activity) {
        mActivity = activity
        val bluetoothManager = activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
        mBluetoothLeScanner = mBluetoothAdapter?.bluetoothLeScanner

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter.isEnabled) {
            //blue id enable
            val bluetoothDoorAddress = mPreferences.bluetoothAddress

            Timber.d("initSetting(), bluetoothDoorAddress = $bluetoothDoorAddress")
            if (bluetoothDoorAddress.isNotEmpty()) {
                val bluetoothLeService = Intent(mActivity, BluetoothLeService::class.java)
                mActivity?.startService(bluetoothLeService)
                mActivity?.bindService(bluetoothLeService, mBluetoothServiceConnection, Context.BIND_AUTO_CREATE)
            }
        }

        mActivity?.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())

        handler.postDelayed(checkBtRunnable, DeviceUtils.BLUETOOTH_CHECK_INTERVAL_TIME)
    }

    fun onActivityPause() {
        mDoorOneTimer?.let {
            it.cancel()
            closeDoorOne()
        }

        mDoorTwoTimer?.let {
            it.cancel()
            closeDoorTwo()
        }
    }

    fun onActivityDestroy() {
        if (mIsBluetoothServiceBind) {
            // mBluetoothService = null;
            mBluetoothLeService?.disconnect()
            mActivity?.unbindService(mBluetoothServiceConnection)
            mIsBluetoothServiceBind = false
        }

        scanLeDevice(false)

        mActivity?.unregisterReceiver(mGattUpdateReceiver)
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_FAIL)
        intentFilter.addAction(BluetoothDevice.ACTION_UUID)
        return intentFilter
    }

    fun bluetoothDoorConnect(address: String?) {
        Timber.d("bluetoothDoor connect, address = $address")

        address?.let {
            mPreferences.bluetoothAddress = address
        }

        val bluetoothService = Intent(mActivity, BluetoothLeService::class.java)
        mActivity?.startService(bluetoothService)
        mActivity?.bindService(bluetoothService, mBluetoothServiceConnection, Context.BIND_AUTO_CREATE)
    }

    fun bluetoothDoorDisconnect() {
        Timber.d("bluetoothDoor disconnect, mIsBluetoothServiceBind = $mIsBluetoothServiceBind")
        if (mIsBluetoothServiceBind) {
            mActivity?.unbindService(mBluetoothServiceConnection)
            mIsBluetoothServiceBind = false
        }
    }

    fun openDoorOne() {
        Timber.d("openDoorOne()")

        // door one open command
        val data = byteArrayOf(
            0xC5.toByte(), 0x04.toByte(), 0x99.toByte(), 0x99.toByte(), 0x99.toByte(),
            0x99.toByte(), 0x99.toByte(), 0x99.toByte(), 0x99.toByte(), 0x99.toByte(), 0xAA.toByte()
        )

        if (DeviceUtils.isBluetoothDisconnected) {
            Timber.d("Bluetooth is not connected already")
            return
        }

        val password = mPreferences.bluetoothPassword
        if (password.length != 8) {
            Timber.d("Password length is not equal to 8, password: $password")
            return
        }

        mBluetoothLeService?.writeBytes(data, password)

        SimpleRxTask.after(mPreferences.closeDoorTimeout * 1000) {
            closeDoorOne()
        }
    }

    fun closeDoorOne() {
        Timber.d("closeDoorOne()")

        // door one close command
        val data = byteArrayOf(
            0xC5.toByte(), 0x06.toByte(), 0x99.toByte(), 0x99.toByte(), 0x99.toByte(),
            0x99.toByte(), 0x99.toByte(), 0x99.toByte(), 0x99.toByte(), 0x99.toByte(), 0xAA.toByte()
        )

        val password = mPreferences.bluetoothPassword
        if (password.length != 8) {
            Timber.d("Password length is not equal to 8, password: $password")
            return
        }

        mBluetoothLeService?.writeBytes(data, password)
    }

    fun openDoorTwo() {
        Timber.d("openDoorTwo()")

        val data = byteArrayOf(
            0xC5.toByte(), 0x05.toByte(), 0x99.toByte(), 0x99.toByte(), 0x99.toByte(),
            0x99.toByte(), 0x99.toByte(), 0x99.toByte(), 0x99.toByte(), 0x99.toByte(), 0xAA.toByte()
        )

        if (DeviceUtils.isBluetoothDisconnected) {
            Timber.d("Bluetooth is not connected already")
            return
        }

        val password = mPreferences.bluetoothPassword
        if (password.length != 8) {
            Timber.d("Password length is not equal to 8, password: $password")
            return
        }

        mBluetoothLeService?.writeBytes(data, password)
    }

    fun closeDoorTwo() {
        Timber.d("closeDoorOne()")

        // door one close command
        val data = byteArrayOf(
            0xC5.toByte(), 0x07.toByte(), 0x99.toByte(), 0x99.toByte(), 0x99.toByte(),
            0x99.toByte(), 0x99.toByte(), 0x99.toByte(), 0x99.toByte(), 0x99.toByte(), 0xAA.toByte()
        )

        val password = mPreferences.bluetoothPassword
        if (password.length != 8) {
            Timber.d("Password length is not equal to 8, password: $password")
            return
        }

        mBluetoothLeService?.writeBytes(data, password)
    }

    fun scanLeDevice(isEnable: Boolean) {
        Timber.d("scanLeDevice(), isEnable: $isEnable")

        if (isEnable) {
            SimpleRxTask.after(SCAN_PERIOD) {
                mScanning = false
                mBluetoothLeScanner?.stopScan(mLeScanCallback)
            }

            mBluetoothLeScanner?.startScan(mLeScanCallback)
            mScanning = true
        } else {
            mBluetoothLeScanner?.stopScan(mLeScanCallback)
            mScanning = false
        }
    }

    private val mLeScanCallback = object: ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Timber.d("onScanFailed(), errorCode: $errorCode")
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            val deviceName = result?.device?.name ?: ""
            if (deviceName.isNotEmpty()) {
                Timber.d("onScanResult(), device name = ${result?.device?.name}")
                Timber.d("onScanResult(), device address = ${result?.device?.address}")
                Timber.d("onScanResult(), device type = ${result?.device?.type}")
                Timber.d("onScanResult(), callbackType = $callbackType")
                scanResultEvent.postValue(MyBluetoothDevice(
                    result?.device?.name,
                    result?.device?.address,
                    result?.device?.type
                ))
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            Timber.d("onBatchScanResults(), result.size(): ${results?.size}")
        }
    }

    private fun reconnect() {
        //Timber.d("reconnect()")

        if (mBluetoothService == null || mBluetoothLeService == null) {
            //Timber.d("[reconnect] service is null")
            return
        }

        Timber.d("[reconnect] bluetooth door address = ${mPreferences.bluetoothAddress}")
        mBluetoothLeService?.connect(mPreferences.bluetoothAddress)
    }
}