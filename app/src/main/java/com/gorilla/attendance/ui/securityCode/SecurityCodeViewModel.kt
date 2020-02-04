package com.gorilla.attendance.ui.securityCode

import androidx.lifecycle.ViewModel
import com.gorilla.attendance.data.model.Acceptances
import com.gorilla.attendance.data.model.NetworkState
import com.gorilla.attendance.ui.common.SharedViewModel
import com.gorilla.attendance.utils.DateUtils
import com.gorilla.attendance.utils.PreferencesHelper
import com.gorilla.attendance.utils.SingleLiveEvent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class SecurityCodeViewModel @Inject constructor(
    securityCodeRepository: SecurityCodeRepository,
    preferences: PreferencesHelper,
    sharedViewModel: SharedViewModel,
    compositeDisposable: CompositeDisposable
) : ViewModel() {

    companion object {
        const val VERIFY_SECURITY_SUCCESS = 0
        const val VERIFY_SECURITY_FAILED = 1
        const val SECURITY_EMPTY = 2
    }

    private val mSecurityCodeRepository: SecurityCodeRepository = securityCodeRepository
    private val mPreferences: PreferencesHelper = preferences
    private val mSharedViewModel: SharedViewModel = sharedViewModel
    private val mCompositeDisposable : CompositeDisposable = compositeDisposable

    var initialLoad = SingleLiveEvent<NetworkState>()

    var verifyCodeResult = SingleLiveEvent<Int>()

    fun verify(securityCode: String?, clockModule: Int) {
        if (securityCode.isNullOrEmpty()) {
            verifyCodeResult.postValue(SECURITY_EMPTY)
            return
        }

        initialLoad.postValue(NetworkState.LOADING)

        val disposableObserver = object : DisposableSingleObserver<List<Acceptances>>() {
            override fun onSuccess(value: List<Acceptances>) {
                Timber.d("List<Acceptances> size = ${value.size}")

                if (value.isNotEmpty()) {
                    // simple choose the first entity, and update clockData

                    if (clockModule == SharedViewModel.MODULE_VISITOR) {
                        val visitor = value.filter {
                            (it.employeeId == null) && (it.deviceToken == mPreferences.tabletToken)
                        }
                        if (visitor.isNotEmpty()) {
                            mSharedViewModel.clockAcceptances = visitor[0]
                            mSharedViewModel.initSingleIdentifier()

                            if (DateUtils.checkVisitorTime(visitor[0].startTime, visitor[0].endTime)) {
                                verifyCodeResult.postValue(VERIFY_SECURITY_SUCCESS)
                            } else {
                                Timber.d("Invalid visit time, delete visitor acceptance from DB")
                                deleteVisitorFromDB(visitor[0])
                                verifyCodeResult.postValue(VERIFY_SECURITY_FAILED)
                            }
                        } else {
                            Timber.d("[Visitor] user not found")
                            verifyCodeResult.postValue(VERIFY_SECURITY_FAILED)
                        }
                    } else {
                        val employee = value.filter {
                            (it.mobileNo == null) && (it.deviceToken == mPreferences.tabletToken)
                        }
                        if (employee.isNotEmpty()) {
                            mSharedViewModel.clockAcceptances = employee[0]
                            mSharedViewModel.initSingleIdentifier()
                            verifyCodeResult.postValue(VERIFY_SECURITY_SUCCESS)
                        } else {
                            Timber.d("[Employee] user not found")
                            verifyCodeResult.postValue(VERIFY_SECURITY_FAILED)
                        }
                    }
                } else {
                    Timber.d("user not found")
                    verifyCodeResult.postValue(VERIFY_SECURITY_FAILED)
                }

                initialLoad.value = NetworkState.LOADED
            }

            override fun onError(e: Throwable) {
                Timber.d("verify security code onError e = $e")
                initialLoad.value = NetworkState.error(e.message)
            }
        }

        if (clockModule != SharedViewModel.MODULE_VISITOR) {
            // Employees
            mCompositeDisposable.add(
                mSecurityCodeRepository.verifyEmployee(securityCode)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(disposableObserver)
            )
        } else {
            mCompositeDisposable.add(
                mSecurityCodeRepository.verifyVisitor(securityCode)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(disposableObserver)
            )
        }
    }

    private fun deleteVisitorFromDB(visitor: Acceptances) {
        Timber.d("deleteVisitorFromDB()")

        val disposableSubscriber = object : DisposableSingleObserver<Int>() {
            override fun onSuccess(value: Int) {
                Timber.d("deleteVisitor onSuccess, index = $value")
            }

            override fun onError(e: Throwable) {
                Timber.d("deleteVisitor onError e = $e")
            }
        }

        mCompositeDisposable.add(
            mSecurityCodeRepository.deleteVisitor(visitor)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(disposableSubscriber)
        )
    }

    override fun onCleared() {
        super.onCleared()
        mCompositeDisposable.clear()
    }

}