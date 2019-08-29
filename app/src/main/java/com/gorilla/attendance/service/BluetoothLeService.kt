package com.gorilla.attendance.service

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.gorilla.attendance.utils.DeviceUtils
import com.gorilla.attendance.utils.PreferencesHelper
import dagger.android.AndroidInjection
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/7/29
 * Description: Bluetooth Low Energy Service
 */
class BluetoothLeService: Service() {

    @Inject
    lateinit var mPreferences: PreferencesHelper

    companion object {
        const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_FAIL = "com.example.bluetooth.le.ACTION_GATT_FAIL"
        const val ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
        const val EXTRA_DATA = "com.example.bluetooth.le.ACTION_EXTRA_DATA"

        // UUID use for bluetooth door lock
        val UUID_NOTIFY = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        val UUID_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    }

    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothGatt: BluetoothGatt? = null

    private var mNotifyCharacteristic: BluetoothGattCharacteristic? = null
    private var mTimer: Timer? = null

    private val mBinder = LocalBinder()

    var timerTask = object: TimerTask() {
        override fun run() {
            mNotifyCharacteristic?.let {
                readCharacteristic(mNotifyCharacteristic)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        Timber.d("onBind()")
        return mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.d("onUnbind()")
        close()
        return super.onUnbind(intent)
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    private fun initialize(): Boolean {
        // For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager
        if (mBluetoothManager == null) {
            mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (mBluetoothManager == null) {
                Timber.d("Unable to initialize BluetoothManager")
                return false
            }
        }

        mBluetoothAdapter = mBluetoothManager?.adapter
        if (mBluetoothAdapter == null) {
            Timber.d("Unable to obtain the BluetoothAdapter from BluetoothManager")
            return false
        }

        return true
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    fun connect(address: String?): Boolean {
        if (!initialize()) {
            Timber.d("initialize failed")
            return false
        }

        if (address.isNullOrEmpty()) {
            Timber.d("Unspecified address")
            return false
        }

/*
        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            LOG.D(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
*/

        val device = mBluetoothAdapter?.getRemoteDevice(address)
        if (device == null) {
            Timber.d("Device not found, unable to connect")
            return false
        }

        // We want to connect to device directly, so we set the autoConnect parameter to false
        mBluetoothGatt?.close()
        mBluetoothGatt = null
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback)

        Timber.d("Trying to create a new connection")
        return true
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    fun disconnect() {
        Timber.d("disconnect()")
        if (mBluetoothAdapter == null) {
            Timber.d("[disconnect], BluetoothAdapter not initialized")
            return
        }

        mBluetoothGatt?.disconnect()
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    private fun close() {
        mBluetoothGatt?.close()
        mBluetoothGatt = null
    }
    
    fun writeBytes(byteData: ByteArray, password: String) {
        var index = 2
        for (byte in password.toByteArray()) {
            byteData[index++] = byte
        }

        Timber.d("byteData[0] = ${byteData[0]}")
        Timber.d("byteData[1] = ${byteData[1]}")
        Timber.d("byteData[2] = ${byteData[2]}")
        Timber.d("byteData[3] = ${byteData[3]}")
        Timber.d("byteData[4] = ${byteData[4]}")
        Timber.d("byteData[5] = ${byteData[5]}")
        Timber.d("byteData[6] = ${byteData[6]}")
        Timber.d("byteData[7] = ${byteData[7]}")
        Timber.d("byteData[8] = ${byteData[8]}")
        Timber.d("byteData[9] = ${byteData[9]}")
        Timber.d("byteData[10] = ${byteData[10]}")
        
        mNotifyCharacteristic?.let { characteristic ->
            characteristic.value = byteData
            mBluetoothGatt?.writeCharacteristic(characteristic)
        }
    }

    fun findService(gattServices: List<BluetoothGattService>) {
        Timber.d("GATT services list size = ${gattServices.size}")

        for (service in gattServices) {
            Timber.d("GATT service UUID = ${service.uuid}")
            Timber.d("UUID_SERVICE = $UUID_SERVICE")

            if (service.uuid.toString().equals(UUID_SERVICE.toString(), ignoreCase = false)) {
                val gattCharacteristics = service.characteristics
                Timber.d("UUID_SERVICE match, size = ${gattCharacteristics.size}")

                for (characteristic in gattCharacteristics) {
                    Timber.d("characteristic UUID = ${characteristic.uuid}")
                    Timber.d("UUID_NOTIFY = $UUID_NOTIFY")

                    if (characteristic.uuid.toString().equals(UUID_NOTIFY.toString(), ignoreCase = false)) {
                        mNotifyCharacteristic = characteristic
                        setCharacteristicNotification(characteristic, true)

                        DeviceUtils.isBluetoothDisconnected = false
                        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                    }
                }
            }
        }
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private val mGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val intentAction: String
            Timber.d("onConnectionStateChange(), oldStatus = $status NewStates = $newState")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    intentAction = ACTION_GATT_DISCONNECTED
                    mBluetoothGatt?.close()
                    mBluetoothGatt = null

                    Timber.d("Disconnected from GATT server.")
                    broadcastUpdate(intentAction)
                } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                    intentAction = ACTION_GATT_CONNECTED
                    broadcastUpdate(intentAction)

                    Timber.d("STATE_CONNECTED mTimer = $mTimer")
                    Timber.d("STATE_CONNECTED timerTask = $timerTask")

                    mTimer = if (mTimer != null) {
                        mTimer?.cancel()
                        val isTimeCancelSuccess = timerTask.cancel()
                        Timber.d("STATE_CONNECTED isTimeCancelSuccess = $isTimeCancelSuccess")
                        Timer()
                    } else {
                        Timer()
                    }

                    timerTask = object: TimerTask() {
                        override fun run() {
                            mNotifyCharacteristic?.let {
                                readCharacteristic(mNotifyCharacteristic)
                            }
                        }
                    }

                    mTimer?.schedule(timerTask, 1000, 2000)

                    Timber.d("Connected to GATT server.")
                    // After connect success, try to find service
                    Timber.d("Attempting to start service discovery:" + mBluetoothGatt?.discoverServices())
                }
            } else {
                intentAction = ACTION_GATT_FAIL
                mBluetoothGatt?.close()
                mBluetoothGatt = null

                Timber.d("ACTION_GATT_FAIL")
                broadcastUpdate(intentAction)

                DeviceUtils.isBluetoothDisconnected = true
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Timber.d("onServicesDiscovered(), status = $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Timber.d("onServicesDiscovered received: $status")
                findService(gatt.services)
            } else {
                if (mBluetoothGatt?.device?.uuids == null) {
                    Timber.d("mBluetoothGatt?.device?.uuids == null")
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            //Timber.d("onCharacteristicRead() status = $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            Timber.d("onCharacteristicChanged()")
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            Timber.d("OnCharacteristicWrite")
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic,
            status: Int) {
            Timber.d("OnCharacteristicWrite()")
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt,
            bd: BluetoothGattDescriptor,
            status: Int
        ) {
            Timber.d("onDescriptorRead()")
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            bd: BluetoothGattDescriptor,
            status: Int
        ) {
            Timber.d("onDescriptorWrite()")
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, a: Int, b: Int) {
            Timber.d("onReadRemoteRssi()")
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt, a: Int) {
            Timber.d("onReliableWriteCompleted()")
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        // For all other profiles, writes the data formatted in HEX.
        val data = characteristic.value
        if (data != null && data.isNotEmpty()) {
            //final StringBuilder stringBuilder = new StringBuilder(data.length);
            //for(byte byteChar : data)
            //    stringBuilder.append(String.format("%02X ", byteChar));
            //intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            intent.putExtra(EXTRA_DATA, String(data))
            //            intent.putExtra(EXTRA_DATA, data);
        }
        sendBroadcast(intent)
    }

    /**
     * Request a read on a given `BluetoothGattCharacteristic`. The read result is reported
     * asynchronously through the `BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)`
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic?) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Timber.d("[readCharacteristic], BluetoothAdapter not initialized")
            DeviceUtils.isBluetoothDisconnected = true
            return
        }

        mBluetoothGatt?.readCharacteristic(characteristic)
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic, enabled: Boolean) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Timber.d("setCharacteristicNotification BluetoothAdapter not initialized")
            return
        }        
        mBluetoothGatt?.setCharacteristicNotification(characteristic, enabled)
        
        /*
        // This is specific to Heart Rate Measurement.
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
        */
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after `BluetoothGatt#discoverServices()` completes successfully.
     *
     * @return A `List` of supported services.
     */
    fun getSupportedGattServices(): List<BluetoothGattService>? {
        return if (mBluetoothGatt == null) null else mBluetoothGatt?.services

    }

    inner class LocalBinder : Binder() {
        val service: BluetoothLeService
            get() = this@BluetoothLeService
    }

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
    }
}