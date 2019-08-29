package com.gorilla.attendance.ui.chooseMode

import android.view.View
import androidx.lifecycle.ViewModel
import androidx.navigation.Navigation
import com.gorilla.attendance.R
import com.gorilla.attendance.data.model.NetworkState
import com.gorilla.attendance.ui.common.SharedViewModel
import com.gorilla.attendance.utils.DeviceUtils
import com.gorilla.attendance.utils.SimpleRxTask
import com.gorilla.attendance.utils.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import javax.inject.Inject

class ChooseModeViewModel @Inject constructor(
    sharedViewModel: SharedViewModel,
    compositeDisposable: CompositeDisposable
) : ViewModel() {

    var initialLoad = SingleLiveEvent<NetworkState>()

    private var mSharedViewModel: SharedViewModel = sharedViewModel
    private var mCompositeDisposable: CompositeDisposable = compositeDisposable

    fun showRFId(view: View) {
        Timber.d("showRFId()")

        if (isSafeToNavigate()) {
            mSharedViewModel.clockMode = SharedViewModel.MODE_RFID
            Navigation.findNavController(view).navigate(R.id.showRFIDFragment)
        }
    }

    fun showSecurity(view: View) {
        Timber.d("showSecurity()")

        if (isSafeToNavigate()) {
            mSharedViewModel.clockMode = SharedViewModel.MODE_SECURITY
            Navigation.findNavController(view).navigate(R.id.showSecurityCodeFragment)
        }
    }

    fun showQrCode(view: View) {
        Timber.d("showQrCode()")

        if (isSafeToNavigate()) {
            mSharedViewModel.clockMode = SharedViewModel.MODE_QR_CODE
            Navigation.findNavController(view).navigate(R.id.showQrCodeFragment)
        }
    }

    fun showFaceRecognition(view: View) {
        Timber.d("showFaceRecognition()")

        if (isSafeToNavigate()) {
            mSharedViewModel.clockMode = SharedViewModel.MODE_FACE_IDENTIFICATION
            Navigation.findNavController(view).navigate(R.id.showFaceIdentificationFragment)
        }
    }

    private fun isSafeToNavigate(): Boolean {
        val diffTime = System.currentTimeMillis() - DeviceUtils.stopFdrOnDestroyTime
        Timber.d("diffTime = $diffTime")

        return diffTime > DeviceUtils.SAFE_FDR_INTERVAL_TIME
    }
}