import com.gorilla.attendance.api.ApiService
import com.gorilla.attendance.mocks.MockApiServiceAsset
import com.gorilla.attendance.mocks.MockRetrofit
import org.junit.Test

import timber.log.Timber

class MockRetrofitTest {
    /**
     * To make sure that Retrofit could intercept API request, and return local mock data
     */
    @Test
    fun deviceLoginApiTest() {
        Timber.d("deviceLoginApiTest(): [start]")

        val retrofit = MockRetrofit()
        val service = retrofit.create(ApiService::class.java)

        retrofit.path = MockApiServiceAsset.DEVICE_LOGIN_TEST
        service.deviceLogin("deviceToken", "deviceType", "deviceIp")
                .test()
                .assertValue {
                    val deviceLoginData = it.body()?.data

                    Timber.d("deviceLoginApiTest(): [success]")
                    if (deviceLoginData != null) {
                        deviceLoginData.locale == "zh_TW"
                        && deviceLoginData.deviceName == "ASUS_TC"
                    } else {
                        false
                    }
                }

        Timber.d("deviceLoginApiTest(): [success]")
    }
}