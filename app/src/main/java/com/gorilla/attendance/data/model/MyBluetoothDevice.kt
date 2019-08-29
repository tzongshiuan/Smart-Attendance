package com.gorilla.attendance.data.model

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/7/30
 * Description:
 */
class MyBluetoothDevice(var deviceName: String?, var address: String?, var deviceType: Int?) {
    override fun equals(other: Any?): Boolean {
        return if (other is MyBluetoothDevice) {
            this.address == other.address
        } else false
    }

    override fun hashCode(): Int {
        var result = deviceName?.hashCode() ?: 0
        result = 31 * result + (address?.hashCode() ?: 0)
        result = 31 * result + (deviceType ?: 0)
        return result
    }
}