package com.gorilla.attendance.ui.chooseMode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.gorilla.attendance.R
import com.gorilla.attendance.databinding.ChooseModeFragmentBinding
import com.gorilla.attendance.di.Injectable
import com.gorilla.attendance.ui.common.BaseFragment
import com.gorilla.attendance.ui.common.SharedViewModel
import com.gorilla.attendance.utils.Constants
import timber.log.Timber

class ChooseModeFragment : BaseFragment(), Injectable {
    private var mBinding: ChooseModeFragmentBinding? = null

    private lateinit var chooseModeViewModel: ChooseModeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = ChooseModeFragmentBinding.inflate(inflater, container, false)
        return mBinding?.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        chooseModeViewModel = ViewModelProviders.of(this, factory).get(ChooseModeViewModel::class.java)

        mBinding?.viewModel = chooseModeViewModel

        sharedViewModel.deviceLoginData.observe(this, Observer {
            Timber.d("sharedViewModel.deviceLoginData it = $it")

            //update UI by chooseModule
            for(i in 0 until (it?.modulesModes?.size ?: 0)){
                if(sharedViewModel.clockModule == it?.modulesModes?.get(i)?.module){
                    for(j in 0 until (it.modulesModes?.get(i)?.modes?.size ?: 0)){
                        when(it.modulesModes?.get(i)?.modes?.get(j)){
                            SharedViewModel.MODE_SECURITY -> {
                                mBinding?.imgSecurityCode?.visibility = View.VISIBLE
                                mBinding?.txtSecurityCode?.visibility = View.VISIBLE
                            }

                            SharedViewModel.MODE_RFID -> {
                                mBinding?.imgIdCard?.visibility = View.VISIBLE
                                mBinding?.txtIdCard?.visibility = View.VISIBLE
                            }

                            SharedViewModel.MODE_QR_CODE -> {
                                mBinding?.imgQrCode?.visibility = View.VISIBLE
                                mBinding?.txtQrCode?.visibility = View.VISIBLE
                            }

//                            SharedViewModel.MODE_FACE_ICON -> {
//                                mBinding?.imgSecurityCode?.visibility = View.VISIBLE
//                                mBinding?.txtSecurityCode?.visibility = View.VISIBLE
//                            }

                            SharedViewModel.MODE_FACE_IDENTIFICATION -> {
                                if (mPreferences.applicationMode == Constants.VERIFICATION_MODE) {
                                    mBinding?.imgFacial?.visibility = View.VISIBLE
                                    mBinding?.txtFacial?.visibility = View.VISIBLE
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume()")

        when (mPreferences.applicationMode) {
            Constants.REGISTER_MODE -> {
                mBinding?.hintText?.text = getString(R.string.choose_registration_mode)

                if (sharedViewModel.clockModule == SharedViewModel.MODULE_VISITOR) {
                    sharedViewModel.changeTitleEvent.postValue(getString(R.string.choose_mode_visitor_registration_title))
                } else {
                    sharedViewModel.changeTitleEvent.postValue(getString(R.string.choose_mode_employee_registration_title))
                }
            }

            Constants.VERIFICATION_MODE -> {
                mBinding?.hintText?.text = getString(R.string.choose_verification_mode)

                if (sharedViewModel.clockModule == SharedViewModel.MODULE_VISITOR) {
                    sharedViewModel.changeTitleEvent.postValue(getString(R.string.choose_mode_visitor_verification_title))
                } else {
                    sharedViewModel.changeTitleEvent.postValue(getString(R.string.choose_mode_employee_verification_title))
                }
            }
        }
    }
}