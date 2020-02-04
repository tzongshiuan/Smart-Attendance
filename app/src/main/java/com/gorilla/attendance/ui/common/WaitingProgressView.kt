package com.gorilla.attendance.ui.common

import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import com.gorilla.attendance.R
import com.gorilla.attendance.databinding.WaitingProgressLayoutBinding
import kotlin.math.abs

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/6/28
 * Description: Kotlin version of progress view (by Shawn Wang)
 */
open class WaitingProgressView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    companion object {
        private const val TIME = 500
        private const val STATUS_FADE_IN = 0
        private const val STATUS_FADE_OUT = 1
        private const val STATUS_NORMAL = 2

        private var mLock: Any? = null
        private var mStatus = STATUS_NORMAL
        private var mBaseAlpha: Float = 0.toFloat()
    }

    init {
        mLock = "WaitingProgressView-lock"
        this.initView(context)
    }

    private var mBinding: WaitingProgressLayoutBinding? = null

    open fun initView(context: Context) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mBinding = WaitingProgressLayoutBinding.inflate(inflater, this, true)
    }

    fun setVisibleImmediate(visibility: Int) {
        mLock?.let {
            synchronized(it) {
                val currentAnimat = animation
                if (currentAnimat != null)
                    animation.cancel()
                clearAnimation()
                mStatus = STATUS_NORMAL
                if (visibility == View.VISIBLE)
                    alpha = 1f
                setVisibility(visibility)
            }
        }
    }


    private fun createNewAnimation(): Animation {
        return object : MyAnimation(context, null) {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                synchronized(mLock!!) {
                    if (isCanceled || isComplete)
                        return
                    if (mStatus == STATUS_FADE_OUT) {
                        alpha = mBaseAlpha - mBaseAlpha * interpolatedTime
                    } else if (mStatus == STATUS_FADE_IN) {
                        alpha = (1 - mBaseAlpha) * interpolatedTime + mBaseAlpha
                    }
                }
            }

        }
    }

    fun setVisibleWithAnimate(visible: Int) {
        synchronized(mLock!!) {
            if (mStatus == STATUS_FADE_IN || mStatus == STATUS_NORMAL) {
                if (visible == View.VISIBLE && visibility == View.VISIBLE)
                    return
            }

            val isRequestInvisible = visible == View.INVISIBLE || visible == View.GONE

            if (mStatus == STATUS_FADE_OUT || mStatus == STATUS_NORMAL) {
                if (isRequestInvisible && (visibility == View.INVISIBLE || visibility == View.GONE))
                    return
            }

            val isNeedFadeIn = !(visible == View.GONE || visible == View.INVISIBLE)

            if (mStatus == STATUS_FADE_OUT || mStatus == STATUS_FADE_IN) {
                val currentAnimation = animation
                currentAnimation?.cancel()
                clearAnimation()
            } else {
                alpha = (if (isNeedFadeIn) 0 else 1).toFloat()
            }
            mBaseAlpha = alpha
            val ani = createNewAnimation()
            val time = (TIME * abs(mBaseAlpha - if (isNeedFadeIn) 1 else 0)).toLong()
            ani.duration = time
            mStatus = if (isNeedFadeIn) STATUS_FADE_IN else STATUS_FADE_OUT
            visibility = View.VISIBLE
            ani.setAnimationListener(object : Animation.AnimationListener {

                override fun onAnimationStart(animation: Animation) {

                }

                override fun onAnimationEnd(animation: Animation) {
                    synchronized(mLock!!) {
                        if ((animation as MyAnimation).isCanceled) {
                            return
                        }
                        if (mStatus == STATUS_FADE_OUT) {
                            visibility = View.GONE
                        } else {
                            alpha = 1.0f
                        }
                        mStatus = STATUS_NORMAL
                        animation.onComplete()
                    }
                }

                override fun onAnimationRepeat(animation: Animation) {

                }
            })
            startAnimation(ani)
        }
    }

    fun setWaitingText(text: String) {
        mBinding?.waitingText?.text = text
    }

    open inner class MyAnimation(context: Context, attributeSet: AttributeSet?) : Animation(context, attributeSet) {
        var isComplete = false
            private set
        var isCanceled = false
            private set

        fun onComplete() {
            isComplete = true
        }

        override fun cancel() {
            isCanceled = true
            isComplete = true
            super.cancel()
        }
    }
}