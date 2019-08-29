package com.gorilla.attendance.di

import android.app.Application
import android.app.Service
import com.gorilla.attendance.AttendanceApp
import dagger.Component
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(
    AndroidSupportInjectionModule::class,
    AppModule::class,
    ActivityBuildersModule::class,
    ServiceModule::class))
interface AppComponent {

//    @Component.Builder
//    interface Builder {
//        @BindsInstance
//        fun application(application: MaterialfennecApp): Builder
//
//        fun build(): AppComponent
//    }

    fun application(): Application
    fun inject(app: AttendanceApp)
}