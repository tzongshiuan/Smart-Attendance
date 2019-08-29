package com.gorilla.attendance.ui.rfid

import androidx.lifecycle.ViewModel
import com.gorilla.attendance.data.model.Acceptances
import com.gorilla.attendance.data.model.NetworkState
import com.gorilla.attendance.ui.common.SharedViewModel
import com.gorilla.attendance.utils.PreferencesHelper
import com.gorilla.attendance.utils.SingleLiveEvent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class RFIDViewModel @Inject constructor(
    rfidRepository: RFIDRepository,
    preferences: PreferencesHelper,
    sharedViewModel: SharedViewModel,
    compositeDisposable: CompositeDisposable
) : ViewModel() {

    companion object {
        const val VERIFY_RFID_SUCCESS = 0
        const val VERIFY_RFID_FAILED = 1
        const val RFID_NULL = 2
    }

    private val mRFIDRepository: RFIDRepository = rfidRepository
    private val mPreferences: PreferencesHelper = preferences
    private var mSharedViewModel: SharedViewModel = sharedViewModel
    private var mCompositeDisposable : CompositeDisposable = compositeDisposable

    var initialLoad = SingleLiveEvent<NetworkState>()

    var verifyCodeResult = SingleLiveEvent<Int>()

    fun verify(rfid: String?, clockModule: Int) {
        if (rfid == null) {
            verifyCodeResult.postValue(RFID_NULL)
            return
        }

        initialLoad.postValue(NetworkState.LOADING)

        val disposableObserver = object : DisposableSingleObserver<List<Acceptances>>() {
            override fun onSuccess(value: List<Acceptances>) {
                Timber.d("List<Acceptances> size = ${value.size}")

                if (value.isNotEmpty()) {
                    // simple choose the first entity, and update clockData

                    if (clockModule == SharedViewModel.MODULE_VISITOR) {
                        Timber.e("Should not enter here, because visitor is not support RFID verification")
                    } else {
                        val employee = value.filter {
                            (it.mobileNo == null) && (it.deviceToken == mPreferences.tabletToken)
                        }
                        if (employee.isNotEmpty()) {
                            mSharedViewModel.clockAcceptances = employee[0]
                            verifyCodeResult.postValue(VERIFY_RFID_SUCCESS)
                        } else {
                            Timber.d("[Employee] user not found")
                            verifyCodeResult.postValue(VERIFY_RFID_FAILED)
                        }
                    }
                } else {
                    Timber.d("user not found")
                    verifyCodeResult.postValue(VERIFY_RFID_FAILED)
                }

                initialLoad.value = NetworkState.LOADED
            }

            override fun onError(e: Throwable) {
                Timber.d("verify RFID onError e = $e")
                initialLoad.value = NetworkState.error(e.message)
            }
        }

        if (clockModule != SharedViewModel.MODULE_VISITOR) {
            // Employees
            mCompositeDisposable.add(
                mRFIDRepository.verifyEmployee(rfid)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(disposableObserver)
            )
        } else {
            Timber.e("Should not enter here, because visitor is not support RFID verification")
        }
    }

    override fun onCleared() {
        super.onCleared()
        mCompositeDisposable.clear()
    }

}