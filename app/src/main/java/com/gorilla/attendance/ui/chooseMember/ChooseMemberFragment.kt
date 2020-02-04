package com.gorilla.attendance.ui.chooseMember

import android.os.Bundle
import android.view.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.gorilla.attendance.R
import com.gorilla.attendance.databinding.ChooseMemberFragmentBinding
import com.gorilla.attendance.di.Injectable
import com.gorilla.attendance.ui.common.BaseFragment
import com.gorilla.attendance.ui.common.SharedViewModel
import com.gorilla.attendance.ui.main.MainActivity
import com.gorilla.attendance.ui.screenSaver.ScreenSaverFragment
import com.gorilla.attendance.utils.Constants
import timber.log.Timber

class ChooseMemberFragment : BaseFragment(), Injectable {
    private var mBinding: ChooseMemberFragmentBinding? = null


    private lateinit var chooseMemberViewModel: ChooseMemberViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = ChooseMemberFragmentBinding.inflate(inflater, container, false)

        mBinding?.imgEmployee?.setOnClickListener {
            Timber.d("imgEmployee click")
            for (i in (0 until (sharedViewModel.deviceLoginData.value?.modulesModes?.size ?: 0))){
                if(sharedViewModel.deviceLoginData.value?.modulesModes?.get(i)?.module != SharedViewModel.MODULE_VISITOR){
                    sharedViewModel.clockModule = sharedViewModel.deviceLoginData.value?.modulesModes?.get(i)?.module ?: 0
                }
            }
            Navigation.findNavController(it).navigate(R.id.showChooseModeFragment)
        }

        mBinding?.imgVisitor?.setOnClickListener {
            Timber.d("imgVisitor click")
            sharedViewModel.clockModule = SharedViewModel.MODULE_VISITOR
            Navigation.findNavController(it).navigate(R.id.showChooseModeFragment)
        }

        return mBinding?.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        chooseMemberViewModel = ViewModelProviders.of(this, factory).get(ChooseMemberViewModel::class.java)

        mBinding?.viewModel = chooseMemberViewModel

        when (mPreferences.applicationMode) {
            Constants.REGISTER_MODE -> {
                mBinding?.imgEmployee?.setImageResource(R.mipmap.ic_employee_register)
                mBinding?.imgVisitor?.setImageResource(R.mipmap.ic_visitor_register)
                mBinding?.hintText?.text = getString(R.string.choose_member_register_hint_text)
            }

            Constants.VERIFICATION_MODE -> {
                mBinding?.imgEmployee?.setImageResource(R.mipmap.ic_employee)
                mBinding?.imgVisitor?.setImageResource(R.mipmap.ic_visitor)
                mBinding?.hintText?.text = getString(R.string.choose_member_hint_text)
            }
        }

//        sharedViewModel.updateLanguageEvent.observe(this, Observer {
//            setTitle()
//        })
    }

    override fun onStart() {
        super.onStart()
        Timber.d("onStart sharedViewModel.clockModule = ${sharedViewModel.clockModule}")
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume()")

        setTitle()

        if (ScreenSaverFragment.isScreenSaverActive) {
            (activity as MainActivity).setToolbarVisible(true)
        }
    }

    private fun setTitle() {
        when (mPreferences.applicationMode) {
            Constants.REGISTER_MODE -> sharedViewModel.changeTitleEvent.postValue(getString(R.string.choose_member_register_title))
            Constants.VERIFICATION_MODE -> sharedViewModel.changeTitleEvent.postValue(getString(R.string.choose_member_title))
        }
    }
}