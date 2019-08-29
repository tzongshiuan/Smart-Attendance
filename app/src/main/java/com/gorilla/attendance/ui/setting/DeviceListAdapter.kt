package com.gorilla.attendance.ui.setting

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gorilla.attendance.R
import com.gorilla.attendance.data.model.MyBluetoothDevice
import com.gorilla.attendance.databinding.SettingDeviceListItemBinding
import com.jakewharton.rxbinding.view.RxView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber


class DeviceListAdapter(val context: Context) : RecyclerView.Adapter<DeviceListAdapter.ViewHolder>() {

    var devices: ArrayList<MyBluetoothDevice> = ArrayList()

    var curAddress: String? = ""

    private val clickSubject = PublishSubject.create<MyBluetoothDevice>()
    val clickEvent: Observable<MyBluetoothDevice> = clickSubject

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Timber.d("onCreateViewHolder()")
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SettingDeviceListItemBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = devices.size

    inner class ViewHolder(private val binding: SettingDeviceListItemBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(device: MyBluetoothDevice) {
            binding.deviceName = device.deviceName
            binding.executePendingBindings()

            if (device.address == curAddress) {
                binding.optionLayout.alpha = 1.0F
                binding.optionLayout.background = context.getDrawable(R.drawable.setting_bluetooth_item_bg)
                binding.optionLayout.background.alpha = 102
                binding.bluetoothIcon.visibility = View.VISIBLE
            } else {
                binding.optionLayout.alpha = 0.4F
                binding.optionLayout.background = null
                binding.bluetoothIcon.visibility = View.GONE
            }

            RxView.clicks(binding.optionItem)
                .subscribe {
                    curAddress = device.address
                    clickSubject.onNext(device)
                    notifyDataSetChanged()
                }
        }
    }

    fun addDevice(device: MyBluetoothDevice) {
        if (device.deviceName.isNullOrEmpty()) {
            return
        }

        if (!devices.contains(device)) {
            devices.add(device)
        }
    }
}