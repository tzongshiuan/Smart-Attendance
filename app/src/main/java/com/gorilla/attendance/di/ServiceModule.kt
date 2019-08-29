package com.gorilla.attendance.di

import com.gorilla.attendance.service.BluetoothLeService
import dagger.Module
import dagger.android.ContributesAndroidInjector


@Module
abstract class ServiceModule {
    @ContributesAndroidInjector
    abstract fun ProvideBluetoothLeService(): BluetoothLeService
}