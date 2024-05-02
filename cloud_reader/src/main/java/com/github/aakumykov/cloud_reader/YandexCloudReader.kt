package com.github.aakumykov.cloud_reader

import com.google.gson.Gson
import com.yandex.disk.rest.json.ApiError
import com.yandex.disk.rest.json.Link
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InputStream

class YandexCloudReader(
    private val authToken: String,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) : CloudReader {


    override suspend fun getDownloadLink(absolutePath: String): Result<String?> {
        return try {
            Result.success(getDownloadLinkDirect(absolutePath))
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    override suspend fun getFileInputStream(absolutePath: String): Result<InputStream?> {
        return try {
            getDownloadLinkDirect(absolutePath).let { url ->

                if (null == url)
                    throw Exception("url is null")

                Request.Builder()
                    .url(url)
                    .build()
                    .let {  request ->
                        okHttpClient.newCall(request).execute().let { response ->
                            when (response.code) {
                                200 -> Result.success(response.body?.byteStream())
                                else -> throw exceptionFromErrorResponse(response)
                            }
                        }
                    }
            }
        } catch (e: IOException) {
            Result.failure(e)
        }
    }


    @Throws(IOException::class)
    private fun getDownloadLinkDirect(absolutePath: String): String? {

        val url = DOWNLOAD_BASE_URL.toHttpUrl().newBuilder()
            .apply {
                addQueryParameter("path", absolutePath)
            }.build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", authToken)
            .build()

        return okHttpClient.newCall(request).execute().use { response ->
            when(response.code) {
                200 -> urlFromResponse(response)
                else -> throw exceptionFromErrorResponse(response)
            }
        }
    }

    private fun urlFromResponse(response: Response): String? {
        return gson.fromJson(response.body?.string(), Link::class.java).href
    }

    private fun exceptionFromErrorResponse(response: Response): Exception {
        return Exception(
            gson.fromJson(response.body?.string(), ApiError::class.java).let {
                "${response.code}: ${it.error}: ${it.description}"
            }
        )
    }

    // TODO: в общий модуль
    private fun linkFromResponse(response: Response): String {
        return gson.fromJson(response.body?.string(), Link::class.java).href
    }

    companion object {
        private const val DISK_BASE_URL = "https://cloud-api.yandex.net/v1/disk"
        private const val RESOURCES_BASE_URL = "${DISK_BASE_URL}/resources"
        private const val DOWNLOAD_BASE_URL = "$RESOURCES_BASE_URL/download"
    }
}