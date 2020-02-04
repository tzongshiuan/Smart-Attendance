package com.gorilla.attendance.ui.common

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.gorilla.attendance.di.Injectable
import com.gorilla.attendance.ui.main.MainActivity
import com.gorilla.attendance.ui.register.RegisterViewModel
import com.gorilla.attendance.utils.DeviceUtils
import com.gorilla.attendance.utils.FdrManager
import com.gorilla.attendance.utils.PreferencesHelper
import com.gorilla.attendance.viewModel.AttendanceViewModelFactory
import javax.inject.Inject

open class BaseFragment : Fragment(), Injectable {

    companion object {
        const val VERIFY_SECURITY_CODE_STATE = 0
        const val VERIFY_FACE_RUNNING_STATE = 1
        const val VERIFY_FACE_FINISH_STATE = 2

        var currentState = VERIFY_SECURITY_CODE_STATE

        var isStartRegister = false
    }

    @Inject
    lateinit var factory: AttendanceViewModelFactory

    @Inject
    lateinit var mPreferences: PreferencesHelper

    @Inject
    lateinit var mFdrManager: FdrManager

    lateinit var sharedViewModel: SharedViewModel
    lateinit var registerViewModel: RegisterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentState = VERIFY_SECURITY_CODE_STATE
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        sharedViewModel = ViewModelProviders.of(this, factory).get(SharedViewModel::class.java)
        registerViewModel = ViewModelProviders.of(this, factory).get(RegisterViewModel::class.java)
    }

    open fun startFdr() {
        DeviceUtils.mFacePngList.clear()
        currentState = VERIFY_FACE_RUNNING_STATE
        (activity as MainActivity).setToolbarVisible(false)

        mFdrManager.mFdrCtrl?.parent?.let {
            (it as ViewGroup).removeView(mFdrManager.mFdrCtrl)
        }
    }

    open fun stopFdr() {
        currentState = VERIFY_FACE_FINISH_STATE
        (activity as MainActivity).setToolbarVisible(true)
    }

    open fun stopFdr(isHideToolbar: Boolean = false) {
        currentState = VERIFY_FACE_FINISH_STATE

        if (!isHideToolbar) {
            (activity as MainActivity).setToolbarVisible(true)
        }
    }

    open fun hideSoftwareKeyboard(customView: View? = null) {
        val imm = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

        //Find the currently focused view, so we can grab the correct window token from it.
        var view =
            customView ?: activity?.currentFocus

        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(activity)
        }

        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}