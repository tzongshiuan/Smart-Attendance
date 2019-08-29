package com.gorilla.attendance.utils

import android.annotation.SuppressLint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/3/28
 * Description: Do something after a little time
 */
@SuppressLint("CheckResult")
object SimpleRxTask {
    fun after(delay: Long, process: () -> Unit) {
        Observable.just(Optional("afterOnIoThread"))
            .delay(delay, TimeUnit.MILLISECONDS)
            .observeOn(Schedulers.io())
            .subscribe {
                process()
            }
    }

    fun onIoThread(process: () -> Unit) {
        Observable.just(Optional("onIOThread"))
            .observeOn(Schedulers.io())
            .subscribe {
                process()
            }
    }

    fun onMain(process: () -> Unit) {
        Observable.just(Optional("onMain"))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                process()
            }
    }

    fun afterOnMain(delay: Long, process: () -> Unit) {
        Observable.just(Optional("afterOnMain"))
            .delay(delay, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                process()
            }
    }

    fun cancelSubscriber(subscriber: Disposable?) {
        subscriber?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
    }

    fun createDelaySubscriber(delay: Long, process: () -> Unit): Disposable {
        return Observable.timer(delay, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .subscribe {
                process()
            }
    }
}