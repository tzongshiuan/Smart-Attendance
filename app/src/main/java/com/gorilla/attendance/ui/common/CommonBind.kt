package com.gorilla.attendance.ui.common

import android.text.format.DateUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.gorilla.attendance.R
import com.gorilla.attendance.utils.Constants
import timber.log.Timber
import java.util.*


object CommonBind{
    private const val AVERAGE_MONTH_IN_MILLIS = DateUtils.DAY_IN_MILLIS * 30

    @JvmStatic
    @BindingAdapter("imageUrl")
    fun bindImage(imageView: ImageView, bannerUrl : String?) {
        Timber.d("imageUrl bannerUrl = $bannerUrl")

        val context = imageView.context

        Glide.with(context)
            .applyDefaultRequestOptions(RequestOptions().placeholder(R.mipmap.ic_launcher))
            .load(bannerUrl)
            .into(imageView)
    }

    @JvmStatic
    @BindingAdapter("showText")
    fun bindText(textView: TextView, text : String?) {
        Timber.d("showText text = $text")
        textView.text = text
    }

    @JvmStatic
    @BindingAdapter("convertDate")
    fun convertDate(textView: TextView, timeStamp : Long?) {
        Timber.d("convertDate timeStamp = $timeStamp")
        if(timeStamp == null){
            return
        }

        val context = textView.context
        val timeMilli = timeStamp.times(1000)
        val now = Date().time
        var delta = now - timeMilli
        val resolution: Long

//        when {
//            delta <= DateUtils.MINUTE_IN_MILLIS -> {
//                resolution = DateUtils.SECOND_IN_MILLIS
//                textView.text = DateUtils.getRelativeTimeSpanString(timeMilli, now, resolution).toString()
//
//            }
//            delta <= DateUtils.HOUR_IN_MILLIS -> {
//                resolution = DateUtils.MINUTE_IN_MILLIS
//                textView.text = DateUtils.getRelativeTimeSpanString(timeMilli, now, resolution).toString()
//
//            }
//            delta <= DateUtils.DAY_IN_MILLIS -> {
//                resolution = DateUtils.HOUR_IN_MILLIS
//                textView.text = DateUtils.getRelativeTimeSpanString(timeMilli, now, resolution).toString()
//            }
//            delta < DateUtils.WEEK_IN_MILLIS -> //days ago
//                textView.text = (delta / DateUtils.DAY_IN_MILLIS).toString() + " " + context.getString(R.string.days_ago)
//            delta < AVERAGE_MONTH_IN_MILLIS ->
//                textView.text = (delta / DateUtils.WEEK_IN_MILLIS).toString() + " " + context.getString(R.string.weeks_ago)
//            delta < DateUtils.YEAR_IN_MILLIS ->
//                textView.text = (delta / AVERAGE_MONTH_IN_MILLIS).toString() + " " + context.getString(R.string.months_ago)
//            else ->
//                textView.text = (delta / DateUtils.YEAR_IN_MILLIS).toString() + " " + context.getString(R.string.years_ago)
//        }
    }

    @JvmStatic
    @BindingAdapter("updateStateTwo")
    fun updateStateTwo(view: TextView, state: Int) {
        if (state >= Constants.REGISTER_STATE_FILL_FORM) {
            view.setTextColor(ContextCompat.getColor(view.context, R.color.black))
            view.alpha = 1.0F
        } else {
            view.setTextColor(ContextCompat.getColor(view.context, R.color.black_trans_30))
            view.alpha = 0.3F
        }
    }

    @JvmStatic
    @BindingAdapter("updateStateTwoText")
    fun updateStateTwoText(view: View, state: Int) {
        if (state >= Constants.REGISTER_STATE_FILL_FORM) {
            view.alpha = 1.0F
        } else {
            view.alpha = 0.3F
        }
    }

    @JvmStatic
    @BindingAdapter("updateStateThree")
    fun updateStateThree(view: TextView, state: Int) {
        if (state >= Constants.REGISTER_STATE_FACE_GET) {
            view.setTextColor(ContextCompat.getColor(view.context, R.color.black))
            view.alpha = 1.0F
        } else {
            view.setTextColor(ContextCompat.getColor(view.context, R.color.black_trans_30))
            view.alpha = 0.3F
        }
    }

    @JvmStatic
    @BindingAdapter("updateStateThreeText")
    fun updateStateThreeText(view: View, state: Int) {
        if (state >= Constants.REGISTER_STATE_FACE_GET) {
            view.alpha = 1.0F
        } else {
            view.alpha = 0.3F
        }
    }

    @JvmStatic
    @BindingAdapter("updateStateFour")
    fun updateStateFour(view: TextView, state: Int) {
        if (state >= Constants.REGISTER_STATE_COMPLETE) {
            view.setTextColor(ContextCompat.getColor(view.context, R.color.black))
            view.alpha = 1.0F
        } else {
            view.setTextColor(ContextCompat.getColor(view.context, R.color.black_trans_30))
            view.alpha = 0.3F
        }
    }

    @JvmStatic
    @BindingAdapter("updateStateFourText")
    fun updateStateFourText(view: View, state: Int) {
        if (state >= Constants.REGISTER_STATE_COMPLETE) {
            view.alpha = 1.0F
        } else {
            view.alpha = 0.3F
        }
    }
}