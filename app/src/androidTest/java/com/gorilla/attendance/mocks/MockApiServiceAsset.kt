package com.gorilla.attendance.mocks

import java.io.*
import java.nio.charset.Charset



object MockApiServiceAsset {
    private val BASE_PATH = "${System.getProperty("user.dir")}\\src\\test\\java\\com\\gorilla\\ivar\\mocks\\data"

    // User API corresponding file path
    const val DEVICE_LOGIN_TEST = "Device_Login_Test"

    // Read data through file path
    fun readFile(path: String): String {
        return loadTestData(path)
//        return file2String(File(path))
    }

    private fun loadTestData(path: String): String {
        val stream = javaClass.classLoader!!.getResourceAsStream(path)

        return BufferedReader(InputStreamReader(stream) as Reader?).use(BufferedReader::readText)
    }

    private fun file2String(f: File, charset: String = "UTF-8"): String {
        return f.readText(Charset.forName(charset))
    }
}