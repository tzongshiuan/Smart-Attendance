package com.gorilla.attendance.ui.setting

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gorilla.attendance.R
import com.gorilla.attendance.databinding.SettingOptionItemBinding
import com.jakewharton.rxbinding.view.RxView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber


class SettingAdapter(val context: Context) : RecyclerView.Adapter<SettingAdapter.ViewHolder>() {

    var items: ArrayList<String>? = null

    var curIndex = 0

    private val clickSubject = PublishSubject.create<String>()
    val clickEvent: Observable<String> = clickSubject

    var isShowBluetoothSetting = true

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items!![position], position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //Timber.d("onCreateViewHolder()")
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SettingOptionItemBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items?.size ?: 0

    fun showBluetooth() {
        isShowBluetoothSetting = true
    }

    fun hideBluetooth() {
        isShowBluetoothSetting = false
    }

    inner class ViewHolder(private val binding: SettingOptionItemBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: String, position: Int) {
            binding.optionText = item
            binding.executePendingBindings()

            if (curIndex == position) {
                binding.optionLayout.alpha = 1.0F
                binding.optionLayout.background = context.getDrawable(R.drawable.setting_option_item_bg)
                binding.optionLayout.background.alpha = 102
            } else {
                binding.optionLayout.alpha = 0.4F
                binding.optionLayout.background = null
            }

            if (position == 5 && !isShowBluetoothSetting) {
                binding.optionLayout.visibility = View.GONE
            } else {
                binding.optionLayout.visibility = View.VISIBLE
            }

            RxView.clicks(binding.optionItem)
                .subscribe {
                    curIndex = position
                    clickSubject.onNext(item)
                    notifyDataSetChanged()
                }
        }
    }
}