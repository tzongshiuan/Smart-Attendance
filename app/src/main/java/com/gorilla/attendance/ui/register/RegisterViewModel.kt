package com.gorilla.attendance.ui.register

import androidx.lifecycle.ViewModel
import com.gorilla.attendance.data.model.*
import com.gorilla.attendance.ui.common.SharedViewModel
import com.gorilla.attendance.utils.Constants
import com.gorilla.attendance.utils.PreferencesHelper
import com.gorilla.attendance.utils.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegisterViewModel @Inject constructor(
    preferences: PreferencesHelper,
    registerRepository: RegisterRepository,
    sharedViewModel: SharedViewModel,
    compositeDisposable: CompositeDisposable
) : ViewModel() {

    private val mPreferences = preferences
    private val mRegisterRepository: RegisterRepository = registerRepository
    private var mSharedViewModel: SharedViewModel = sharedViewModel
    private var mCompositeDisposable : CompositeDisposable = compositeDisposable

    /****************  Register data ***************************/
    var employeeRegisterData: EmployeeRegisterData? = null
    var visitorRegisterData: VisitorRegisterData? = null

    var registerStateEvent = SingleLiveEvent<Int>()

    val errorToastEvent = SingleLiveEvent<String>()

    fun checkSecurityCode(securityCode: String) {
        if (securityCode.isEmpty()) {
            registerStateEvent.postValue(RegisterFormState.EMPTY_SECURITY_CODE)
            return
        }

        if (mSharedViewModel.clockModule != SharedViewModel.MODULE_VISITOR) {
            checkEmployeeSecurityCode(securityCode)
        } else {
            checkVisitorSecurityCode(securityCode)
        }
    }

    private fun checkEmployeeSecurityCode(securityCode: String) {
        Timber.d("checkEmployeeSecurityCode(), securityCode: $securityCode")

        mSharedViewModel.isUserProfileExist = false
        val disposableObserver = object : DisposableObserver<CheckUserResponse>() {
            override fun onNext(response: CheckUserResponse) {
                Timber.d("checkEmployeeSecurityCode onNext value = $response")

                when {
                    response.status == ApiResponseModel.STATUS_SUCCESS -> {
                        Timber.d("Find match employee")
                        registerStateEvent.postValue(RegisterFormState.INVALID_SECURITY_CODE)

                        mSharedViewModel.isUserProfileExist = true

                        val data = response.data
                        mSharedViewModel.retrainId = data?.id ?: "unknown id"
                        employeeRegisterData = EmployeeRegisterData().also {
                            it.name = data?.firstName
                            it.employeeId = data?.employeeId
                        }
                    }

                    response.status == ApiResponseModel.STATUS_ERROR
                            && response.error?.message == ErrorMessageTable.user_notFound_1 -> {
                        Timber.d("Employee not found, error: ${response.error?.message}")
                        registerStateEvent.postValue(RegisterFormState.VALID_SECURITY_CODE)
                    }

                    else -> {
                        Timber.d("Unknown error, error: ${response.error?.message}")
                        registerStateEvent.postValue(RegisterFormState.VALID_SECURITY_CODE)
                    }
                }
            }

            override fun onError(e: Throwable) {
                Timber.d("checkEmployeeSecurityCode onError e = $e")
                errorToastEvent.postValue(e.message)
            }

            override fun onComplete() {
                Timber.d("checkEmployeeSecurityCode onComplete")
            }
        }

        mCompositeDisposable.add(
            mRegisterRepository.checkUser(mPreferences.tabletToken, Constants.USER_TYPE_EMPLOYEE, securityCode, null)
                .observeOn(Schedulers.io())
                .subscribeWith(disposableObserver)
        )
    }

    private fun checkVisitorSecurityCode(securityCode: String) {
        Timber.d("checkVisitorSecurityCode(), securityCode: $securityCode")

        mSharedViewModel.isUserProfileExist = false
        val disposableObserver = object : DisposableObserver<CheckUserResponse>() {
            override fun onNext(response: CheckUserResponse) {
                Timber.d("checkVisitorSecurityCode onNext value = $response")

                when {
                    response.status == ApiResponseModel.STATUS_SUCCESS -> {
                        Timber.d("Find match visitor")
                        registerStateEvent.postValue(RegisterFormState.INVALID_SECURITY_CODE)

                        mSharedViewModel.isUserProfileExist = true

                        val data = response.data
                        mSharedViewModel.retrainId = data?.id ?: "unknown id"
                        visitorRegisterData = VisitorRegisterData().also {
                            it.name = data?.firstName
                            it.mobileNo = data?.mobileNo
                        }
                    }

                    response.status == ApiResponseModel.STATUS_ERROR
                            && response.error?.message == ErrorMessageTable.user_notFound_1 -> {
                        Timber.d("Visitor not found, error: ${response.error?.message}")
                        registerStateEvent.postValue(RegisterFormState.VALID_SECURITY_CODE)
                    }

                    else -> {
                        Timber.d("Unknown error, error: ${response.error?.message}")
                        registerStateEvent.postValue(RegisterFormState.VALID_SECURITY_CODE)
                    }
                }
            }

            override fun onError(e: Throwable) {
                Timber.d("checkVisitorSecurityCode onError e = $e")
                errorToastEvent.postValue(e.message)
            }

            override fun onComplete() {
                Timber.d("checkVisitorSecurityCode onComplete")
            }
        }

        mCompositeDisposable.add(
            mRegisterRepository.checkUser(mPreferences.tabletToken, Constants.USER_TYPE_VISITOR, securityCode, null)
                .observeOn(Schedulers.io())
                .subscribeWith(disposableObserver)
        )
    }

    fun checkRfid(rfid: String) {
        if (rfid.isEmpty()) {
            registerStateEvent.postValue(RegisterFormState.EMPTY_SECURITY_CODE)
            return
        }

        if (mSharedViewModel.clockModule != SharedViewModel.MODULE_VISITOR) {
            checkEmployeeRfid(rfid)
        } else {
            // not support now
            //checkVisitorSecurityCode(securityCode)
        }
    }

    private fun checkEmployeeRfid(rfid: String) {
        Timber.d("checkEmployeeRfid(), rfid: $rfid")

        mSharedViewModel.isUserProfileExist = false
        val disposableObserver = object : DisposableObserver<CheckUserResponse>() {
            override fun onNext(response: CheckUserResponse) {
                Timber.d("checkEmployeeRfid onNext value = $response")

                when {
                    response.status == ApiResponseModel.STATUS_SUCCESS -> {
                        Timber.d("Find match employee")
                        registerStateEvent.postValue(RegisterFormState.INVALID_SECURITY_CODE)

                        mSharedViewModel.isUserProfileExist = true

                        val data = response.data
                        mSharedViewModel.retrainId = data?.id ?: "unknown id"
                        employeeRegisterData = EmployeeRegisterData().also {
                            it.name = data?.firstName
                            it.employeeId = data?.employeeId
                        }
                    }

                    response.status == ApiResponseModel.STATUS_ERROR
                            && response.error?.message == ErrorMessageTable.user_notFound_1 -> {
                        Timber.d("Employee not found, error: ${response.error?.message}")
                        registerStateEvent.postValue(RegisterFormState.VALID_SECURITY_CODE)
                    }

                    else -> {
                        Timber.d("Unknown error, error: ${response.error?.message}")
                        registerStateEvent.postValue(RegisterFormState.VALID_SECURITY_CODE)
                    }
                }
            }

            override fun onError(e: Throwable) {
                Timber.d("checkEmployeeRfid onError e = $e")
                errorToastEvent.postValue(e.message)
            }

            override fun onComplete() {
                Timber.d("checkEmployeeRfid onComplete")
            }
        }

        mCompositeDisposable.add(
            mRegisterRepository.checkUser(mPreferences.tabletToken, Constants.USER_TYPE_EMPLOYEE, null, rfid)
                .observeOn(Schedulers.io())
                .subscribeWith(disposableObserver)
        )
    }

    override fun onCleared() {
        super.onCleared()
        mCompositeDisposable.clear()
    }

}