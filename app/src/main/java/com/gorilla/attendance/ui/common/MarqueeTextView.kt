package com.gorilla.attendance.ui.common

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Scroller
import androidx.appcompat.widget.AppCompatTextView

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/7/19
 * Description:
 */
class MarqueeTextView(context: Context, attrs: AttributeSet?, defStyle: Int) :
    AppCompatTextView(context, attrs, defStyle) {

    private var mScroller: Scroller? = null
    // the X offset when paused
    private var mXPaused = 0
    // whether it's being paused
    private var isPaused = true
    //milliseconds for a round of scrolling.
    private var roundDuration = 20000
    var speed = 1

    private var isFirst = true

    constructor(context: Context): this(context, null) {
        setSingleLine()
        ellipsize = null
        visibility = View.INVISIBLE
    }

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, android.R.attr.textViewStyle) {
        setSingleLine()
        ellipsize = null
        visibility = View.INVISIBLE
    }

    init {
        setSingleLine()
        ellipsize = null
        visibility = View.INVISIBLE
    }

    fun startScroll() {
        mXPaused = -1 * width
        isPaused = true
        resumeScroll()
    }

    fun resumeScroll() {
        if (!isPaused) {
            return
        }
        // Do not know why it would not scroll sometimes
        // if setHorizontallyScrolling is called in constructor.
        setHorizontallyScrolling(true)
        // use LinearInterpolator for steady scrolling
        mScroller = Scroller(this.context, LinearInterpolator())
        setScroller(mScroller)
        val scrollingLen = calculateScrollingLen()
        val distance = scrollingLen - (width + mXPaused)
        val duration: Int = if (isFirst) {
            isFirst = false
            0
        } else {
            ((roundDuration/speed).toDouble() * distance.toDouble() * 1.00000 / scrollingLen).toInt()
        }
        visibility = View.VISIBLE
        mScroller!!.startScroll(mXPaused, 0, distance, 0, duration)
        invalidate()
        isPaused = false
    }

    /**
     * calculate the scrolling length of the text in pixel
     *
     * @return the scrolling length in pixels
     */
    private fun calculateScrollingLen(): Int {
        val tp = paint
        val rect: Rect? = Rect()
        val strTxt = text.toString()
        tp.getTextBounds(strTxt, 0, strTxt.length, rect)

        return rect!!.width() + width
    }

    /**
     * pause scrolling the text
     */
    fun pauseScroll() {
        if (null == mScroller) {
            return
        }

        if (isPaused) {
            return
        }

        isPaused = true
        // abortAnimation sets the current X to be the final X,
        // and sets isFinished to be true
        // so current position shall be saved
        mXPaused = mScroller!!.getCurrX()
        mScroller!!.abortAnimation()
    }

    override fun computeScroll() {
        super.computeScroll()
        if (null == mScroller) return
        if (mScroller!!.isFinished() && !isPaused) {
            this.startScroll()
        }
    }
}