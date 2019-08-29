package com.gorilla.attendance.ui.register

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Patterns
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.HtmlCompat
import com.gorilla.attendance.R
import com.gorilla.attendance.data.model.RegisterFormState
import com.gorilla.attendance.data.model.VisitorRegisterData
import com.gorilla.attendance.databinding.VisitorRegisterLayoutBinding
import com.gorilla.attendance.utils.DateUtils
import com.gorilla.attendance.utils.PreferencesHelper

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/7/22
 * Description:
 */
class VisitorRegForm (context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    private lateinit var mBinding: VisitorRegisterLayoutBinding

    private lateinit var mPreferences: PreferencesHelper

    private lateinit var mRegisterViewModel: RegisterViewModel

    //private var isSecurityValid = false

//    var rfid = ""
//        set(value) {
//            mBinding.isShowRFID = true
//            mBinding.rfidEditText.setText(value)
//            field = value
//        }

    var securityCode = ""
        set(value) {
            mBinding.visitorSecurityEditText.setText(value)
            field = value
        }

    init {
        this.initView(context)
    }

    private fun initView(context: Context) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mBinding = VisitorRegisterLayoutBinding.inflate(inflater, this, true)

        initNecessaryHintUI()
    }

    private fun initNecessaryHintUI() {
        val necessaryPrefix = HtmlCompat.fromHtml(resources.getString(R.string.necessary_prefix_str), 0)

        val name = TextUtils.concat(necessaryPrefix, resources.getString(R.string.form_visitor_name))
        mBinding.nameLabel.text = name

        val phone = TextUtils.concat(necessaryPrefix, resources.getString(R.string.form_phone_number))
        mBinding.phoneLabel.text = phone

        val email = TextUtils.concat(necessaryPrefix, resources.getString(R.string.form_email))
        mBinding.emailLabel.text = email

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

        mBinding.nameEditText.text.clear()
        mBinding.titleEditText.text.clear()
        mBinding.companyEditText.text.clear()
        mBinding.phoneEditText.text.clear()
        mBinding.emailEditText.text.clear()
        mBinding.visitorSecurityEditText.text.clear()

        // security code edit text
//        RxTextView.textChanges(mBinding.visitorSecurityEditText)
//            .subscribe {
//                isSecurityValid = false
//                mBinding.securityCheckResult.visibility = View.INVISIBLE
//            }

        // just for test
//        mBinding.nameEditText.setText("hsuan")
//        mBinding.titleEditText.setText("Engineer")
//        mBinding.companyEditText.setText("Gorilla Technology Inc.")
//        mBinding.phoneEditText.setText("0930565748")
//        mBinding.emailEditText.setText("tsunghsuanlai@gorilla-technology.com")
//        mBinding.visitorSecurityEditText.setText("07121234")
    }

    fun setSecurityEditTextDisable(code: String) {
        securityCode = code
        mBinding.securityLabel.alpha = 0.5F
        mBinding.visitorSecurityEditText.alpha = 0.5F
        mBinding.visitorSecurityEditText.isEnabled = false
    }

    fun checkRegistrationForm(): Boolean {
        when {
            mBinding.nameEditText.text.toString().isEmpty() -> {
                mRegisterViewModel.registerStateEvent.postValue(RegisterFormState.EMPTY_VISITOR_NAME)
                return false
            }

            mBinding.phoneEditText.text.toString().isEmpty() -> {
                mRegisterViewModel.registerStateEvent.postValue(RegisterFormState.EMPTY_MOBILE_PHONE)
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

            mBinding.visitorSecurityEditText.text.toString().isEmpty() -> {
                mRegisterViewModel.registerStateEvent.postValue(RegisterFormState.EMPTY_SECURITY_CODE)
                return false
            }
        }

        // set data into registerViewModel
        mRegisterViewModel.visitorRegisterData = VisitorRegisterData().also {
            it.deviceToken = mPreferences.tabletToken
            it.mobileNo = mBinding.phoneEditText.text.toString()
            it.name = mBinding.nameEditText.text.toString()
            it.email = mBinding.emailEditText.text.toString()
            it.securityCode = mBinding.visitorSecurityEditText.text.toString()
            it.createTime = DateUtils.nowDateTime2Str()

            mBinding.companyEditText.text.toString().also { company ->
                if (company.isNotEmpty()) {
                    it.company = company
                }
            }

            mBinding.titleEditText.text.toString().also { title ->
                if (title.isNotEmpty()) {
                    it.title = title
                }
            }
        }

        return true
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

}