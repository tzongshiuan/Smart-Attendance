package com.gorilla.attendance.api

import com.gorilla.attendance.data.model.*
import io.reactivex.Observable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @GET("SmartEnterprise/api/V1_1beta/device/login?")
    fun deviceLogin(@Query("deviceToken") deviceToken : String?,
                    @Query("deviceType") deviceType : String?,
                    @Query("deviceIP") deviceIp : String?): Observable<Response<DeviceLogin>>

    /**
     * about registration
     */
    @GET("SmartEnterprise/api/V1_1beta/user?")
    fun checkUser(@Query("deviceToken") deviceToken: String?, @Query("type") type: String?
                  , @Query("securityCode") securityCode: String?, @Query("rfid") rfid: String?): Observable<Response<CheckUserResponse>>

    @GET("SmartEnterprise/api/V1_1beta/employee?")
    fun checkEmployee(@Query("deviceToken") deviceToken: String?, @Query("employeeId") employeeId: String?): Observable<Response<CheckUserResponse>>

    @GET("SmartEnterprise/api/V1_1beta/visitor?")
    fun checkVisitor(@Query("deviceToken") deviceToken: String?, @Query("mobileNo") mobileNo: String?): Observable<Response<CheckUserResponse>>

    @POST("SmartEnterprise/api/V1_1beta/user/employee-register-v2")
    fun registerEmployee(@Body body: EmployeeRegisterData): Observable<Response<RegisterUserResponse>>

    @POST("SmartEnterprise/api/V1_1beta/user/visitor-register-v2")
    fun registerVisitor(@Body body: VisitorRegisterData): Observable<Response<RegisterUserResponse>>
    //////////////////////////////////////////////////////////////////////////////////////////////////////////



    @GET("SmartEnterprise/api/V1_1beta/device/employees?")
    fun getDeviceEmployees(@Query("deviceToken") deviceToken: String?): Observable<Response<DeviceEmployees>>

    @GET("SmartEnterprise/api/V1_1beta/device/visitors?")
    fun getDeviceVisitors(@Query("deviceToken") deviceToken: String?): Observable<Response<DeviceVisitors>>

    @GET("/SmartEnterprise/api/V1_1beta/BAP/identities?")
    fun getDeviceIdentities(@Query("deviceToken") deviceToken: String?): Observable<Response<DeviceIdentities>>

    @GET("/SmartEnterprise/api/V1_1beta/BAP/identitiesbyTime?")
    fun getUpdateDeviceIdentities(@Query("deviceToken") deviceToken: String?, @Query("time") time: String?): Observable<Response<DeviceIdentities>>

    @GET("SmartEnterprise/api/V1_1beta/device/marquees?")
    fun getDeviceMarquees(@Query("deviceToken") deviceToken: String?): Observable<Response<DeviceMarquees>>

    @GET("SmartEnterprise/api/V1_1beta/device/videos?")
    fun getDeviceVideos(@Query("deviceToken") deviceToken: String?): Observable<Response<DeviceVideos>>

    @POST("/SmartEnterprise/api/V1_1beta/BAP/identify")
    fun bapIdentify(@Body body: BapIdentifyData): Observable<Response<ClockResponse>>

    @POST("/SmartEnterprise/api/V1_1beta/BAP/verify")
    fun bapVerify(@Body body: BapVerifyData): Observable<Response<ClockResponse>>

    @POST("/SmartEnterprise/api/V1_1beta/BAP/enroll")
    fun bapEnroll(@Body body: BapEnrollData): Observable<Response<ClockResponse>>

    // Clock record API
    @POST("/SmartEnterprise/api/V1_1beta/attendance/records")
    fun clockAttendance(@Body deviceRecordsData: DeviceRecordsData?): Observable<Response<ClockResponse>>

    @POST("/SmartEnterprise/api/V1_1beta/access/records")
    fun clockAccess(@Body deviceRecordsData: DeviceRecordsData?): Observable<Response<ClockResponse>>

    @POST("/SmartEnterprise/api/V1_1beta/visitor/records")
    fun clockVisitor(@Body deviceRecordsData: DeviceRecordsData?): Observable<Response<ClockResponse>>

    @POST("/SmartEnterprise/api/V1_1beta/visitor/access/records")
    fun clockVisitorAccess(@Body deviceRecordsData: DeviceRecordsData?): Observable<Response<ClockResponse>>

    // Clock unrecognized API
    @POST("/SmartEnterprise/api/V1_1beta/attendance/unrecognizedLog")
    fun unrecognizedAttendance(@Body deviceRecordsData: DeviceUnrecognizedData?): Observable<Response<ClockResponse>>

    @POST("/SmartEnterprise/api/V1_1beta/access/unrecognizedLog")
    fun unrecognizedAccess(@Body deviceRecordsData: DeviceUnrecognizedData?): Observable<Response<ClockResponse>>

    @POST("/SmartEnterprise/api/V1_1beta/visitor/unrecognizedLog")
    fun unrecognizedVisitor(@Body deviceRecordsData: DeviceUnrecognizedData?): Observable<Response<ClockResponse>>

    @POST("/SmartEnterprise/api/V1_1beta/visitor/access/unrecognizedLog")
    fun unrecognizedVisitorAccess(@Body deviceRecordsData: DeviceUnrecognizedData?): Observable<Response<ClockResponse>>
}