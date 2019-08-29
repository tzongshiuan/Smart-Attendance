package com.gorilla.attendance.utils

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/6/26
 * Description:
 */
class XAPKFile(isMain: Boolean, fileVersion: Int, fileSize: Long) {
    var mIsMain: Boolean = isMain
    var mFileVersion: Int = fileVersion
    var mFileSize: Long = fileSize
}