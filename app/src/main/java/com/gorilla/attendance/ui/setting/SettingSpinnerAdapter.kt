package com.gorilla.attendance.ui.setting

import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.gorilla.attendance.R

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/6/24
 * Description:
 */
class SettingSpinnerAdapter(val context: Context, private val listItems: Array<String>) : BaseAdapter() {

    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    var curPosition = 0

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val vh: ItemRowHolder
        if (convertView == null) {
            view = mInflater.inflate(R.layout.setting_spinner_item, parent, false)
            vh = ItemRowHolder(view)
            view.tag = vh
        } else {
            view = convertView
            vh = view.tag as ItemRowHolder
        }

        vh.label.text = listItems[position]
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        if (view == null) {
            view = mInflater.inflate(R.layout.setting_spinner_dropdown_item, null)
        }

        val dropLayout = view?.findViewById<ConstraintLayout>(R.id.dropDownLayout)
        val dropText = view?.findViewById<TextView>(R.id.dropDownText)

        if (position == curPosition) {
            dropLayout?.background = context.getDrawable(R.drawable.setting_spinner_dropdown_bg2)
            dropText?.setTextColor(context.getColor(R.color.white))
        } else {
            dropLayout?.background = context.getDrawable(R.drawable.setting_spinner_dropdown_bg)
            dropText?.setTextColor(context.getColor(R.color.black))
        }
        dropText?.text = listItems[position]

        return view ?: super.getDropDownView(position, convertView, parent)
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getCount(): Int {
        return listItems.size
    }

    private class ItemRowHolder(row: View?) {
        val label: TextView = row?.findViewById(R.id.spinnerText) as TextView
    }
}