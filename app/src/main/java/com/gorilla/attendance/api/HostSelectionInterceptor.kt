package com.gorilla.attendance.api

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

open class HostSelectionInterceptor: Interceptor {
    @Volatile private var host: HttpUrl? = null

    fun setHost(url: String) {
        this.host = HttpUrl.parse(url)
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val newRequest = host?.let {
            //Timber.d("old url = ${chain.request().url()}")
            val newUrl = chain.request().url().newBuilder()
                .scheme(it.scheme())
                .host(it.url().toURI().host)
                .port(it.port())
                .build()
            //Timber.d("new url = ${newUrl}")

            return@let chain.request().newBuilder()
                .url(newUrl)
                .build()
        }

        return chain.proceed(newRequest!!)
    }
}