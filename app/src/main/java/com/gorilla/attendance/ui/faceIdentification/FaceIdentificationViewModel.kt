package com.gorilla.attendance.ui.faceIdentification

import androidx.lifecycle.ViewModel
import com.gorilla.attendance.data.model.NetworkState
import com.gorilla.attendance.ui.common.SharedViewModel
import com.gorilla.attendance.utils.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class FaceIdentificationViewModel @Inject constructor(
    faceIdentificationRepository: FaceIdentificationRepository,
    sharedViewModel: SharedViewModel,
    compositeDisposable: CompositeDisposable
) : ViewModel() {

    companion object {
    }

    private val mFaceIdentificationRepository: FaceIdentificationRepository = faceIdentificationRepository
    private var mSharedViewModel: SharedViewModel = sharedViewModel
    private var mCompositeDisposable : CompositeDisposable = compositeDisposable

    var initialLoad = SingleLiveEvent<NetworkState>()

    override fun onCleared() {
        super.onCleared()
        mCompositeDisposable.clear()
    }
}