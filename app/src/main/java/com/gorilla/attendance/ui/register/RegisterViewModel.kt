package com.gorilla.attendance.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorilla.attendance.data.model.*
import com.gorilla.attendance.ui.common.SharedViewModel
import com.gorilla.attendance.utils.Constants
import com.gorilla.attendance.utils.PreferencesHelper
import com.gorilla.attendance.utils.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch
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
        viewModelScope.launch {
            if (securityCode.isEmpty()) {
                registerStateEvent.postValue(RegisterFormState.EMPTY_SECURITY_CODE)
                return@launch
            }

            if (mSharedViewModel.clockModule != SharedViewModel.MODULE_VISITOR) {
                checkEmployeeSecurityCode(securityCode)
            } else {
                checkVisitorSecurityCode(securityCode)
            }
        }
    }

    private suspend fun checkEmployeeSecurityCode(securityCode: String) {
        Timber.d("checkEmployeeSecurityCode(), securityCode: $securityCode")

        mSharedViewModel.isUserProfileExist = false

        val response = mRegisterRepository.checkUser(mPreferences.tabletToken, Constants.USER_TYPE_EMPLOYEE, securityCode, null)

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

    private suspend fun checkVisitorSecurityCode(securityCode: String) {
        Timber.d("checkVisitorSecurityCode(), securityCode: $securityCode")

        mSharedViewModel.isUserProfileExist = false

        val response = mRegisterRepository.checkUser(mPreferences.tabletToken, Constants.USER_TYPE_VISITOR, securityCode, null)

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

    fun checkRfid(rfid: String) {
        viewModelScope.launch {
            if (rfid.isEmpty()) {
                registerStateEvent.postValue(RegisterFormState.EMPTY_SECURITY_CODE)
                return@launch
            }

            if (mSharedViewModel.clockModule != SharedViewModel.MODULE_VISITOR) {
                checkEmployeeRfid(rfid)
            } else {
                // not support now
                //checkVisitorSecurityCode(securityCode)
            }
        }
    }

    private suspend fun checkEmployeeRfid(rfid: String) {
        Timber.d("checkEmployeeRfid(), rfid: $rfid")

        mSharedViewModel.isUserProfileExist = false

        val response = mRegisterRepository.checkUser(mPreferences.tabletToken, Constants.USER_TYPE_EMPLOYEE, null, rfid)

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

    override fun onCleared() {
        super.onCleared()
        mCompositeDisposable.clear()

        val mUser = User(name = "kotlin", pwd = "123456")
        val (name, pwd) = mUser

        val pair = Pair(1, 2)
        val triple = Triple(1, 2, 3)

        val list = pair.toList()
    }

}

sealed class Operation {
    class Add(val value: Int): Operation()
    class Subtract(val value: Int): Operation()
    class Multiply(val value: Int): Operation()
    class Divide(val value: Int): Operation()
}

fun execute(x: Int, op: Operation) = when (op) {
    is Operation.Add ->      x + op.value
    is Operation.Subtract -> x - op.value
    is Operation.Multiply -> x * op.value
    is Operation.Divide ->   x / op.value
}

data class User(val name: String, val pwd: String)
