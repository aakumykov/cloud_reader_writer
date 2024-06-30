package com.github.aakumykov.cloud_writer

import okhttp3.OkHttpClient
import org.junit.Test

class YandexCloudWriterTest {

    /*private val yandexCloudWriter: YandexCloudWriter by lazy {
        YandexCloudWriter(okhttpClient(), Gson(), )
    }*/

    @Test
    fun whenCreateDirThenSuccess() {

    }

    private fun okhttpClient(): OkHttpClient = OkHttpClient.Builder().build()
}