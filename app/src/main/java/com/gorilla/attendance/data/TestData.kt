package com.gorilla.attendance.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.gorilla.attendance.utils.DeviceUtils
import java.io.ByteArrayOutputStream


/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/7/3
 * Description:
 */
object TestData {
    val FACE_BYTE_ARRAY: ByteArray
    get() {
        return readTestFaceImageByteArray()
    }

    val FACE_IMAGE: String
    get() {
        return readTestFaceImage()
    }

    private fun readTestFaceImageByteArray(): ByteArray {
        val filePath = DeviceUtils.SD_CARD_APP_FACE_IMAGE + "/test_face.jpg"
        val bitmap = BitmapFactory.decodeFile(filePath)
        return getBytesFromBitmap(bitmap)
    }

    private fun readTestFaceImage(): String {
        val filePath = DeviceUtils.SD_CARD_APP_FACE_IMAGE + "/test_face.jpg"
        val bitmap = BitmapFactory.decodeFile(filePath)
        val imageList = getBytesFromBitmap(bitmap)

        return Base64.encodeToString(imageList, Base64.NO_WRAP)
    }

    private fun getBytesFromBitmap(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        return stream.toByteArray()
    }
}