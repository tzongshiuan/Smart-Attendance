package com.gorilla.attendance.data.model


/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/7/3
 * Description: Handle all error messages
 */
object ErrorMessageTable {

    // Http stats: 400 BAD_REQUEST
    var device_badRequest_1 = "Without deviceToken"
    var device_badRequest_2 = "Without deviceIP"
    var device_badRequest_3 = "Without deviceType"
    var device_badRequest_4 = "Without mobileUid"
    var user_badRequest_1  = "Without recognized id"
    var user_badRequest_2  = "Without user type"
    var user_badRequest_3  = "Invalid user type"
    var user_badRequest_4  = "Without employeeId or security code"
    var user_badRequest_5  = "Without mobileNo"
    var user_badRequest_6  = "Without image list or list is null"
    var user_badRequest_7  = "User not found and cannot insert the user"
    var user_badRequest_8  = "Without email or socialSecurityNo"
    var user_badRequest_9  = "Without orderNo or visitorId"
    var user_badRequest_10 = "RFID or Employee ID or E-mail already exist"
    var user_badRequest_11 = "Create time was earlier than the data in database"
    var user_badRequest_12 = "RFID or Mobile No or E-mail already exist"
    var user_badRequest_13 = "recognized id is used by more than two users"
    var user_badRequest_14 = "Without username"
    var user_badRequest_15 = "Without password"
    var user_badRequest_16 = "Without pinCode"
    var user_badRequest_17 = "Without emergencyCode"
    var user_badRequest_18 = "Same pinCode and emergencyCode"
    var record_badRequest_1 = "Without records"
    var record_badRequest_2 = "Without errorLogs"
    var record_badRequest_3 = "Error Visitor Identification"
    var record_badRequest_4 = "Error Visitor Card Info"
    var BAP_badRequest_2 = "Without image list or list is null"
    var employee_badRequest_1 = "Without recognize id"
    var system_badRequest_1 = "Without createTime"

    // Http stats: 404 NOT FOUND
    var device_notFound_1	= "Device not found"
    var user_notFound_1	    = "User not found"
    var user_notFound_2	    = "User not enlists yet"
    var record_notFound_1	= "Record not found"
    var BAP_notFound_1	    = "Member not found"
    var employee_notFound_1	= "Employee not found"
    var employee_notFound_2	= "Without face id"

    // Http stats: 403 FORBIDDEN
    var user_forbidden_1	= "User verified failed"
    var user_forbidden_2	= "Wrong username or password"
    var user_forbidden_3	= "Wrong pinCode"

    // Http stats: 500 INTERNAL_SERVER_ERROR
    var BAP_serverError_1	= "Connect BAP Server Error"
    var unknown_error	    = "Unknown Error"
}