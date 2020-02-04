package com.gorilla.attendance.ui.register

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Patterns
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.HtmlCompat
import com.gorilla.attendance.R
import com.gorilla.attendance.data.model.EmployeeRegisterData
import com.gorilla.attendance.data.model.RegisterFormState
import com.gorilla.attendance.databinding.EmployeeRegisterLayoutBinding
import com.gorilla.attendance.utils.DateUtils
import com.gorilla.attendance.utils.PreferencesHelper
import java.util.regex.Pattern

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/7/22
 * Description:
 */
class EmployeeRegForm (context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    private lateinit var mBinding: EmployeeRegisterLayoutBinding

    private lateinit var mPreferences: PreferencesHelper

    private lateinit var mRegisterViewModel: RegisterViewModel

    //var isSecurityValid = false

    var rfid = ""
        set(value) {
            mBinding.isShowRFID = true
            mBinding.rfidEditText.setText(value)
            field = value
        }

    var securityCode = ""
        set(value) {
            mBinding.employeeSecurityEditText.setText(value)
            field = value
        }

    private var isShowRFID = false
        set(value) {
            mBinding.isShowRFID = value
            field = value
        }

    init {
        this.initView(context)

        isShowRFID = false
    }

    private fun initView(context: Context) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mBinding = EmployeeRegisterLayoutBinding.inflate(inflater, this, true)

        //initNecessaryHintUI()
    }

    private fun initNecessaryHintUI() {
        val necessaryPrefix = HtmlCompat.fromHtml(resources.getString(R.string.necessary_prefix_str), 0)

        val id = TextUtils.concat(necessaryPrefix, resources.getString(R.string.form_employee_id))
        mBinding.idLabel.text = id

        val name = TextUtils.concat(necessaryPrefix, resources.getString(R.string.form_employee_name))
        mBinding.nameLabel.text = name

        val email = TextUtils.concat(necessaryPrefix, resources.getString(R.string.form_email))
        mBinding.emailLabel.text = email

        val password = TextUtils.concat(necessaryPrefix, resources.getString(R.string.form_password))
        mBinding.passwordLabel.text = password

        val security = TextUtils.concat(necessaryPrefix, resources.getString(R.string.form_security_code))
        mBinding.securityLabel.text = security
    }

//    fun showSecurityCheckResult(isValid: Boolean) {
//        val text = if (isValid) {
//            isSecurityValid = true
//            HtmlCompat.fromHtml(resources.getString(R.string.form_valid_security_code), 0)
//        } else {
//            isSecurityValid = false
//            HtmlCompat.fromHtml(resources.getString(R.string.form_invalid_security_code), 0)
//        }
//
//        mBinding.securityCheckResult.text = text
//        mBinding.securityCheckResult.visibility = View.VISIBLE
//        mBinding.formLayout.fullScroll(ScrollView.FOCUS_DOWN)
//    }

    fun initUI(preferences: PreferencesHelper, registerViewModel: RegisterViewModel) {
        mPreferences = preferences
        mRegisterViewModel = registerViewModel

        mBinding.idEditText.text.clear()
        mBinding.nameEditText.text.clear()
        mBinding.emailEditText.text.clear()
        mBinding.passwordEditText.text.clear()
        mBinding.employeeSecurityEditText.text.clear()

        // security code edit text
//        RxTextView.textChanges(mBinding.employeeSecurityEditText)
//            .subscribe {
//                isSecurityValid = false
//                mBinding.securityCheckResult.visibility = View.INVISIBLE
//            }

        // just for test
//        mBinding.idEditText.setText("hsuan3")
//        mBinding.nameEditText.setText("hsuan3")
//        mBinding.emailEditText.setText("tsunghsuanlai3@gorilla-technology.com")
//        mBinding.passwordEditText.setText("Shiuan6391@0606")
//        mBinding.employeeSecurityEditText.setText("07121234")
    }

    fun setSecurityEditTextDisable(code: String) {
        securityCode = code
        mBinding.securityLabel.alpha = 0.5F
        mBinding.employeeSecurityEditText.alpha = 0.5F
        mBinding.employeeSecurityEditText.isEnabled = false
    }

    fun clearUI() {
        mBinding.idEditText.text.clear()
        mBinding.nameEditText.text.clear()
        mBinding.emailEditText.text.clear()
        mBinding.passwordEditText.text.clear()
        mBinding.employeeSecurityEditText.text.clear()
    }

    fun checkRegistrationForm(): Boolean {
        when {
            mBinding.idEditText.text.toString().isEmpty() -> {
                mRegisterViewModel.registerStateEvent.postValue(RegisterFormState.EMPTY_EMPLOYEE_ID)
                return false
            }

            mBinding.nameEditText.text.toString().isEmpty() -> {
                mRegisterViewModel.registerStateEvent.postValue(RegisterFormState.EMPTY_EMPLOYEE_NAME)
                return false
            }

            mBinding.emailEditText.text.toString().isEmpty() -> {
                mRegisterViewModel.registerStateEvent.postValue(RegisterFormState.EMPTY_EMAIL)
                return false
            }

            !isValidEmail(mBinding.emailEditText.text.toString()) -> {
                mRegisterViewModel.registerStateEvent.postValue(RegisterFormState.INVALID_EMAIL_FORMAT)
                return false
            }

            mBinding.passwordEditText.text.toString().isEmpty() -> {
                mRegisterViewModel.registerStateEvent.postValue(RegisterFormState.EMPTY_PASSWORD)
                return false
            }

            // No longer restrict password format
//            !isValidPassword(mBinding.passwordEditText.text.toString()) -> {
//                mRegisterViewModel.registerStateEvent.postValue(RegisterFormState.INVALID_PASSWORD_FORMAT)
//                return false
//            }

            mBinding.employeeSecurityEditText.text.toString().isEmpty() -> {
                mRegisterViewModel.registerStateEvent.postValue(RegisterFormState.EMPTY_SECURITY_CODE)
                return false
            }
        }

        // set data into registerViewModel
        mRegisterViewModel.employeeRegisterData = EmployeeRegisterData().also {
            it.deviceToken = mPreferences.tabletToken
            it.employeeId = mBinding.idEditText.text.toString()
            it.name = mBinding.nameEditText.text.toString()
            it.email = mBinding.emailEditText.text.toString()
            it.password = mBinding.passwordEditText.text.toString()
            it.rfid = mBinding.rfidEditText.text.toString()   // register by RFID only
            it.securityCode = mBinding.employeeSecurityEditText.text.toString()
            it.createTime = DateUtils.nowDateTime2Str()
        }

        return true
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     *  ^                 # start-of-string
        (?=.*[0-9])       # a digit must occur at least once
        (?=.*[a-z])       # a lower case letter must occur at least once
        (?=.*[A-Z])       # an upper case letter must occur at least once
        (?=.*[@#$%^&+=])  # a special character must occur at least once you can replace with your special characters
        (?=\\S+$)         # no whitespace allowed in the entire string
        .{8,}             # anything, at least eight places though
        $                 # end-of-string
     */
    private fun isValidPassword(password: String): Boolean {
        val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$"
        val pattern = Pattern.compile(passwordPattern)
        val matcher = pattern.matcher(password)

        return matcher.matches()
    }
}