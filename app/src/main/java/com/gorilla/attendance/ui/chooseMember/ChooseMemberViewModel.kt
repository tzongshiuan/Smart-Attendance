package com.gorilla.attendance.ui.chooseMember

import androidx.lifecycle.ViewModel
import com.gorilla.attendance.data.model.DeviceLoginData
import com.gorilla.attendance.data.model.NetworkState
import com.gorilla.attendance.utils.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class ChooseMemberViewModel : ViewModel {
    private val mChooseMemberRepository: ChooseMemberRepository
    private var mCompositeDisposable : CompositeDisposable

    var initialLoad = SingleLiveEvent<NetworkState>()

    var deviceLoginData = SingleLiveEvent<DeviceLoginData>()

    @Inject
    constructor(chooseMemberRepository: ChooseMemberRepository, compositeDisposable : CompositeDisposable){
        mChooseMemberRepository = chooseMemberRepository
        mCompositeDisposable = compositeDisposable

    }

    override fun onCleared() {
        super.onCleared()
        mCompositeDisposable.clear()
    }
}
