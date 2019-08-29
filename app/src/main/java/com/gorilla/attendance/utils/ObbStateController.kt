package com.gorilla.attendance.utils

import android.util.Log
import timber.log.Timber

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/6/26
 * Description:
 */
class ObbStateController {

    val listState = ArrayList<Int>()
    val currentStateLiveEvent = SingleLiveEvent<Int>()
    val completeStateResult = SingleLiveEvent<Boolean>()

    private var mStartStateIndex = -1
    private var mCurrentStateIndex = -1

    fun onStateEnd(success: Boolean) {
        if (mCurrentStateIndex < 0) {
            return
        }

        Timber.log(Log.DEBUG, "onStateEnd, state: ${listState[mCurrentStateIndex]}, result: $success")

        if (success) {
            if (mCurrentStateIndex == listState.lastIndex) {
                Timber.d("Complete all states success")
                completeStateResult.postValue(true)
            } else {
                mCurrentStateIndex++
                currentStateLiveEvent.postValue(listState[mCurrentStateIndex])
            }
        } else {
            if (mStartStateIndex == 0) {
                Timber.d("Failed to start OBB manager at the beginning")
                completeStateResult.postValue(false)
            } else {
                startFromState(listState[mCurrentStateIndex - 1])
            }
        }
    }

    fun getCurrentState(): Int {
        if (mCurrentStateIndex < 0) {
            return -1
        }
        return listState[mCurrentStateIndex]
    }

    fun startFromState(state: Int) {
        mStartStateIndex = listState.indexOf(state)
        mCurrentStateIndex = mStartStateIndex
        currentStateLiveEvent.postValue(state)
    }

    fun cancel() {
        mStartStateIndex = -1
        mCurrentStateIndex = -1
    }
}