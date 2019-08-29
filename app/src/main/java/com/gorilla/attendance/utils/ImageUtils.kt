package com.gorilla.attendance.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/7/2
 * Description:
 */
class ImageUtils {
    companion object {
        fun saveImages(num: Int, pngList: List<ByteArray>, name: String) {
            val now = Date()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateStr = sdf.format(now)

            for (i in 0 until num) {
                val dir = File(DeviceUtils.SD_CARD_APP_FACE_IMAGE)
                if (!dir.exists()) {
                    dir.mkdirs()
                }

                try {
                    val file = File(
                        dir.absolutePath + String.format(
                            "/%s_$i-%s.png",
                            name,
                            dateStr
                        )
                    )
                    val fos = FileOutputStream(file)
                    fos.write(pngList[i])
                    fos.flush()
                    fos.close()

                } catch (e: Exception) {
                }
            }
        }

        fun getBitmapFromBytes(bytes: ByteArray): Bitmap {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }
}