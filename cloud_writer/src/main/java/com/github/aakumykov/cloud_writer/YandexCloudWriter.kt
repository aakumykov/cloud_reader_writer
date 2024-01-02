package com.github.aakumykov.cloud_writer

import com.google.gson.Gson
import com.yandex.disk.rest.json.Link
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException

class YandexCloudWriter @AssistedInject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    @Assisted private val authToken: String
) : BasicCloudWriter()
{
    @Throws(IOException::class,
        CloudWriter.UnsuccessfulResponseException::class,
        CloudWriter.AlreadyExistsException::class)
    override fun createDir(parentDirName: String, childDirName: String) {

        val dirName = fixDirSeparators(parentDirName + CloudWriter.DS + childDirName)

        val url = BASE_URL.toHttpUrl().newBuilder().apply {
            addQueryParameter("path", dirName)
        }.build()

        val requestBody = "".toRequestBody(null)

        val request: Request = Request.Builder()
            .header("Authorization", authToken)
            .url(url)
            .put(requestBody)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                when (response.code) {
                    409 -> throw CloudWriter.AlreadyExistsException(response.code, dirName)
                    else -> throw unsuccessfulResponseException(response)
                }
            }
        }
    }


    // TODO: игнорировать, если существует
    @Throws(IOException::class, CloudWriter.UnsuccessfulResponseException::class)
    override fun createDirWithParents(parentDirName: String, childDirName: String) {
        with(childDirName) {
            this.trim()
            iterateThroughDirHierarchy(this) { nextDirName ->
                createDir(parentDirName, nextDirName)
            }
        }
    }


    @Throws(IOException::class, CloudWriter.UnsuccessfulResponseException::class)
    override fun putFile(file: File, targetDirPath: String, overwriteIfExists: Boolean) {

        val fullTargetPath = "${targetDirPath}/${file.name}".replace(Regex("[/]+"),"/")

        val uploadURL = getURLForUpload(fullTargetPath, overwriteIfExists)

        putFileReal(file, uploadURL)
    }


    @Throws(IOException::class, CloudWriter.UnsuccessfulResponseException::class)
    private fun getURLForUpload(targetFilePath: String, overwriteIfExists: Boolean): String {


        val url = "$BASE_URL/upload".toHttpUrl().newBuilder()
            .apply {
                addQueryParameter("path", targetFilePath)
                addQueryParameter("overwrite", overwriteIfExists.toString())
            }.build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", authToken)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful)
                return gson.fromJson(response.body?.string(), Link::class.java).href
            else
                throw unsuccessfulResponseException(response)
        }
    }



    @Throws(IOException::class, CloudWriter.UnsuccessfulResponseException::class)
    private fun putFileReal(file: File, uploadURL: String) {

        val requestBody: RequestBody = file.asRequestBody(DEFAULT_MEDIA_TYPE.toMediaType())

        val fileUploadRequest = Request.Builder()
            .put(requestBody)
            .url(uploadURL)
            .build()

        okHttpClient.newCall(fileUploadRequest).execute().use { response ->
            if (!response.isSuccessful) throw unsuccessfulResponseException(response)
        }
    }


    private fun unsuccessfulResponseException(response: Response): Throwable {
        return CloudWriter.UnsuccessfulResponseException(response.code, response.message)
    }


    companion object {
        private const val BASE_URL = "https://cloud-api.yandex.net/v1/disk/resources"
        private const val DEFAULT_MEDIA_TYPE = "application/octet-stream"
    }


}