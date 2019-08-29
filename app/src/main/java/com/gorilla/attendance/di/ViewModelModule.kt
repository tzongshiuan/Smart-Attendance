package com.gorilla.attendance.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gorilla.attendance.ui.chooseMember.ChooseMemberViewModel
import com.gorilla.attendance.ui.chooseMode.ChooseModeViewModel
import com.gorilla.attendance.ui.common.SharedViewModel
import com.gorilla.attendance.ui.faceIdentification.FaceIdentificationViewModel
import com.gorilla.attendance.ui.main.MainViewModel
import com.gorilla.attendance.ui.qrCode.QrCodeViewModel
import com.gorilla.attendance.ui.register.RegisterViewModel
import com.gorilla.attendance.ui.rfid.RFIDViewModel
import com.gorilla.attendance.ui.screenSaver.ScreenSaverViewModel
import com.gorilla.attendance.ui.securityCode.SecurityCodeViewModel
import com.gorilla.attendance.ui.setting.SettingViewModel
import com.gorilla.attendance.viewModel.AttendanceViewModelFactory
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {
    @Binds
    abstract fun bindViewModelFactory(factory: AttendanceViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    abstract fun bindMainViewModel(mainViewModel: MainViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SharedViewModel::class)
    abstract fun bindSharedViewModel(sharedViewModel: SharedViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ChooseMemberViewModel::class)
    abstract fun bindChooseMemberViewModel(chooseMemberViewModel: ChooseMemberViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ChooseModeViewModel::class)
    abstract fun bindChooseModeViewModel(chooseModeViewModel: ChooseModeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RFIDViewModel::class)
    abstract fun bindRFIDViewModel(rfidViewModel: RFIDViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SecurityCodeViewModel::class)
    abstract fun bindSecurityCodeViewModel(securityCodeViewModel: SecurityCodeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(QrCodeViewModel::class)
    abstract fun bindQrCodeViewModel(qrCodeViewModel: QrCodeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FaceIdentificationViewModel::class)
    abstract fun bindFaceIdentificationViewModel(faceIdentificationViewModel: FaceIdentificationViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingViewModel::class)
    abstract fun bindSettingViewModel(settingiewModel: SettingViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ScreenSaverViewModel::class)
    abstract fun bindScreenSaverViewModel(screenSaverViewModel: ScreenSaverViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RegisterViewModel::class)
    abstract fun bindRegisterViewModel(registerViewModel: RegisterViewModel): ViewModel
}