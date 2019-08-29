package com.gorilla.attendance.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.gorilla.attendance.api.ApiService
import com.gorilla.attendance.api.HostSelectionInterceptor
import com.gorilla.attendance.data.db.*
import com.gorilla.attendance.utils.*
import com.gorilla.attendance.utils.networkChecker.NetworkChecker
import dagger.Module
import dagger.Provides
import io.reactivex.disposables.CompositeDisposable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module(includes = arrayOf(ViewModelModule::class))
class AppModule {
    private val application: Application

    constructor(application: Application) {
        this.application = application

    }

    @Provides
    @Singleton
    fun provideApplication(): Application {
        return this.application
    }

    @Provides
    @Singleton
    fun provideContext(application: Application): Context {
        return application
    }

    @Provides
    @Singleton
    fun provideHostSelectionInterceptor(): HostSelectionInterceptor {
        return HostSelectionInterceptor()
    }

    @Provides
    @Singleton
    fun provideApiService(preferencesHelper : PreferencesHelper, hostSelectionInterceptor: HostSelectionInterceptor): ApiService {
        val interceptor : HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
            this.level = HttpLoggingInterceptor.Level.BODY
        }

        val client : OkHttpClient = OkHttpClient.Builder().apply {
            this
                .addInterceptor(hostSelectionInterceptor)
                .addInterceptor(interceptor)
                .connectTimeout(300, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
        }.build()

        try {
            Timber.d("provideApiService, serverIp: ${preferencesHelper.serverIp}")
            return Retrofit.Builder()
                .baseUrl("http://" + preferencesHelper.serverIp)
                .addConverterFactory(GsonConverterFactory.create())
//                .addCallAdapterFactory(LiveDataCallAdapterFactory())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(client)   //show log
                .build()
                .create(ApiService::class.java)
        } catch (e: Exception) {
            return Retrofit.Builder()
                .baseUrl("http://192.168.11.178")   // and error server ip
                .addConverterFactory(GsonConverterFactory.create())
//                .addCallAdapterFactory(LiveDataCallAdapterFactory())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(client)   //show log
                .build()
                .create(ApiService::class.java)
        }
    }

    @Provides
    fun provideCompositeDisposable(): CompositeDisposable {
        return CompositeDisposable()
    }

    @Provides
    @Singleton
    fun providePreferencesHelper(myPreferences: AppPreferences): PreferencesHelper {
        return myPreferences
    }

    @Singleton
    @Provides
    fun provideDb(app: Application): AttendanceDb {
        return Room
            .databaseBuilder(app, AttendanceDb::class.java, "attendance.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Singleton
    @Provides
    fun provideDeviceLoginDao(db: AttendanceDb): DeviceLoginDao {
        return db.deviceLoginDao()
    }

    @Singleton
    @Provides
    fun provideDeviceEmployeeDao(db: AttendanceDb): DeviceEmployeeDao {
        return db.deviceEmployeeDao()
    }

    @Singleton
    @Provides
    fun provideDeviceVisitorDao(db: AttendanceDb): DeviceVisitorDao {
        return db.deviceVisitorDao()
    }

    @Singleton
    @Provides
    fun provideClockDao(db: AttendanceDb): ClockDao {
        return db.clockDao()
    }

    @Singleton
    @Provides
    fun provideDeviceIdentitiesDao(db: AttendanceDb): DeviceIdentitiesDao {
        return db.deviceIdentitiesDao()
    }

    @Singleton
    @Provides
    fun provideDeviceMarqueesDao(db: AttendanceDb): DeviceMarqueesDao {
        return db.deviceMarqueesDao()
    }

    @Singleton
    @Provides
    fun provideDeviceVideosDao(db: AttendanceDb): DeviceVideosDao {
        return db.deviceVideosDao()
    }

    @Singleton
    @Provides
    fun provideObbManager(compositeDisposable: CompositeDisposable): ObbManager {
        return ObbManager(compositeDisposable)
    }

    @Singleton
    @Provides
    fun provideFdrManager(context: Context, myPreferences: AppPreferences): FdrManager {
        return FdrManager.getInstance(context, myPreferences)
    }

    @Singleton
    @Provides
    fun provideWebSocketManager(myPreferences: AppPreferences): WebSocketManager {
        return WebSocketManager(myPreferences)
    }

    @Singleton
    @Provides
    fun provideBtLeManager(myPreferences: AppPreferences): BtLeManager {
        return BtLeManager(myPreferences)
    }

    @Singleton
    @Provides
    fun provideNetworkChecker(context: Context): NetworkChecker {
        return NetworkChecker(context)
    }

    @Singleton
    @Provides
    fun provideOfflineIdentifyManager(): OfflineIdentifyManager {
        return OfflineIdentifyManager()
    }

    @Singleton
    @Provides
    fun provideVideoManager(myPreferences: AppPreferences): VideoManager {
        return VideoManager(myPreferences)
    }

    @Singleton
    @Provides
    fun provideNfcManager(): NfcManager {
        return NfcManager()
    }

    @Singleton
    @Provides
    fun provideUsbRelayManager(myPreferences: AppPreferences): UsbRelayManager {
        return UsbRelayManager(myPreferences)
    }
}