package com.gorilla.attendance.utils

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/6/20
 * Description:
 */
data class Optional<T>(val value: T?)
fun <T> T?.asOptional() = Optional(this)