package com.github.aakumykov.cloud_reader

import com.google.gson.Gson
import com.yandex.disk.rest.json.Link
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.InputStream

class YandexCloudReader(
    private val authToken: String,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) : CloudReader {

    override suspend fun getFileInputStream(absolutePath: String): Result<InputStream> {

        val url = DOWNLOAD_BASE_URL.toHttpUrl().newBuilder()
            .apply {
                addQueryParameter("path", absolutePath)
            }.build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", authToken)
            .build()

        return try {
            okHttpClient.newCall(request).execute().let { response ->
                if (response.isSuccessful)
                    response.body?.byteStream()?.let { Result.success(it) }
                        ?: Result.failure(Exception("Empty response body"))
                else
                    Result.failure(Exception())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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