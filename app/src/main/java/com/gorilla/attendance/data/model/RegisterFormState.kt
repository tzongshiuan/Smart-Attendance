package com.gorilla.attendance.data.model

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/7/23
 * Description:
 */
class RegisterFormState {
    companion object {
        // common part
        const val EMPTY_SECURITY_CODE = 0
        const val VALID_SECURITY_CODE = 1
        const val INVALID_SECURITY_CODE = 2
        const val MUST_CHECK_SECURITY_CODE = 3
        const val EMPTY_EMAIL = 4
        const val INVALID_EMAIL_FORMAT = 5

        // visitor part
        const val EMPTY_VISITOR_NAME = 6
        const val EMPTY_MOBILE_PHONE = 7

        // employee part
        const val EMPTY_EMPLOYEE_ID = 8
        const val EMPTY_EMPLOYEE_NAME = 9
        const val EMPTY_PASSWORD = 10
        const val INVALID_PASSWORD_FORMAT = 11

        // duplicate
        const val EMPLOYEE_EXIST_HINT = 12
        const val VISITOR_EXIST_HINT = 13
    }
}