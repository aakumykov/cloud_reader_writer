package com.github.aakumykov.cloud_reader.yandex_cloud_reader

import com.github.aakumykov.cloud_reader.CloudReader
import com.google.gson.Gson
import com.google.gson.JsonPrimitive
import com.yandex.disk.rest.json.ApiError
import com.yandex.disk.rest.json.Link
import com.yandex.disk.rest.json.Resource
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class YandexCloudReader(
    private val authToken: String,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().build(),
    private val gson: Gson = Gson()
) : CloudReader {

    override suspend fun getDownloadLink(absolutePath: String): Result<String> {
        return try {
            Result.success(getDownloadLinkDirect(absolutePath))
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        }
    }

    override suspend fun getFileInputStream(absolutePath: String): Result<InputStream> {
        return try {
            getDownloadLinkDirect(absolutePath).let { url ->

                Request.Builder()
                    .url(url)
                    .build()
                    .let {  request ->
                        okHttpClient.newCall(request).execute().let { response ->
                            when (response.code) {
                                200 -> Result.success(streamFromResponse(response))
                                else -> throw exceptionFromErrorResponse(response)
                            }
                        }
                    }
            }
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        }
    }

    override suspend fun fileExists(absolutePath: String): Result<Boolean> {
        return try {
            getFileInfoDirect(absolutePath).let { Result.success(true) }
        }
        catch (e: FileNotFoundException) {
            Result.success(false)
        }
        catch (t: Throwable) {
            Result.failure(t)
        }
    }


    @Throws(IllegalArgumentException::class)
    private fun httpRequest(url: HttpUrl, paramsMap: Map<HttpMethod, EmptyHttpParam>): Request {
        return Request.Builder()
            .url(url)
            .apply {
                paramsMap.keys.forEach { methodName ->
                    when(methodName) {
                        HttpMethod.HEADER -> paramsMap[methodName]?.let { header(it.name, it.value) }
                        HttpMethod.GET -> get()
                    }
                }
            }
            .build()
    }

    @Throws(IOException::class, IllegalArgumentException::class)
    private fun getDownloadLinkDirect(absolutePath: String): String {

        val request = httpRequest(
            urlWithPath(DOWNLOAD_BASE_URL, absolutePath),
            mapOf(HttpMethod.HEADER to HttpParam("Authorization", authToken))
        )

        return okHttpClient.newCall(request).execute().use { response ->
            when(response.code) {
                200 -> urlFromResponse(response)
                else -> throw exceptionFromErrorResponse(response)
            }
        }
    }

    @Throws(FileNotFoundException::class, IOException::class, IllegalArgumentException::class)
    private fun getFileInfoDirect(absolutePath: String): Resource {

        val request = httpRequest(
            urlWithPath(RESOURCES_BASE_URL, absolutePath),
            mapOf(
                HttpMethod.HEADER to HttpParam("Authorization", authToken),
                HttpMethod.GET to EmptyHttpParam(),
            )
        )

        return okHttpClient.newCall(request).execute().use { response ->
            when(response.code) {
                200 -> resourceFromResponse(response)
                404 -> throw FileNotFoundException("File not found: $absolutePath")
                else -> throw exceptionFromErrorResponse(response)
            }
        }
    }

    private fun urlWithPath(baseUrl: String, absolutePath: String): HttpUrl {
        return baseUrl.toHttpUrl().newBuilder()
            .apply {
                addQueryParameter("path", absolutePath)
            }.build()
    }


    private fun resourceFromResponse(response: Response): Resource {
        return response.body?.let {
            return gson.fromJson(it.string(), Resource::class.java)
        } ?: throw nullResponseBodyException()
    }


    @Throws(IllegalArgumentException::class)
    private fun urlFromResponse(response: Response): String {
        return response.body?.let {
            return gson.fromJson(it.string(), Link::class.java).href
        } ?: throw nullResponseBodyException()
    }


    @Throws(IllegalArgumentException::class)
    private fun streamFromResponse(response: Response): InputStream {
        return response.body?.byteStream() ?: throw nullResponseBodyException()
    }


    private fun exceptionFromErrorResponse(response: Response): Exception {
        return Exception(
            gson.fromJson(response.body?.string(), ApiError::class.java).let {
                "${response.code}: ${it.error}: ${it.description}"
            }
        )
    }


    private fun nullResponseBodyException() = IllegalArgumentException("Null response body.")


    companion object {
        private const val DISK_BASE_URL = "https://cloud-api.yandex.net/v1/disk"
        private const val RESOURCES_BASE_URL = "$DISK_BASE_URL/resources"
        private const val DOWNLOAD_BASE_URL = "$RESOURCES_BASE_URL/download"
    }

    private enum class HttpMethod {
        HEADER, GET
    }


    private open class EmptyHttpParam(val name: String = "", val value: String = "")
    private class HttpParam(name: String, value: String) : EmptyHttpParam(name, value)


}