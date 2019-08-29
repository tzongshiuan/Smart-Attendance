package com.gorilla.attendance.di

import com.gorilla.attendance.ui.chooseMember.ChooseMemberFragment
import com.gorilla.attendance.ui.chooseMode.ChooseModeFragment
import com.gorilla.attendance.ui.common.BaseFragment
import com.gorilla.attendance.ui.faceIdentification.FaceIdentificationFragment
import com.gorilla.attendance.ui.qrCode.QrCodeFragment
import com.gorilla.attendance.ui.rfid.RFIDFragment
import com.gorilla.attendance.ui.screenSaver.ScreenSaverFragment
import com.gorilla.attendance.ui.securityCode.SecurityCodeFragment
import com.gorilla.attendance.ui.setting.SettingFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class FragmentBuildersModule {
    @ContributesAndroidInjector
    abstract fun contributeBaseFragment(): BaseFragment

    @ContributesAndroidInjector
    abstract fun contributeChooseMemberFragment(): ChooseMemberFragment

    @ContributesAndroidInjector
    abstract fun contributeChooseModeFragment(): ChooseModeFragment

    @ContributesAndroidInjector
    abstract fun contributeRFIDFragment(): RFIDFragment

    @ContributesAndroidInjector
    abstract fun contributeSecurityCodeFragment(): SecurityCodeFragment

    @ContributesAndroidInjector
    abstract fun contributeQrCodeFragment(): QrCodeFragment

    @ContributesAndroidInjector
    abstract fun contributeFaceIdentificationFragment(): FaceIdentificationFragment

    @ContributesAndroidInjector
    abstract fun contributeSettingFragment(): SettingFragment

    @ContributesAndroidInjector
    abstract fun contributeScreenSaverFragment(): ScreenSaverFragment

//    @ContributesAndroidInjector
//    abstract fun contributeSearchSuggestionDialogFragment(): SearchSuggestionDialogFragment
}