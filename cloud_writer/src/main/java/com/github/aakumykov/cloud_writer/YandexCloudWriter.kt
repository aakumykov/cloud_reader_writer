package com.github.aakumykov.cloud_writer

import android.util.Log
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
    // TODO: проверить с разными аргументами
    @Throws(
        IOException::class,
        CloudWriter.UnsuccessfulResponseException::class,
        CloudWriter.AlreadyExistsException::class
    )
    override fun createDir(parentDirName: String, childDirName: String) {
        if (!childDirName.contains(CloudWriter.DS)) createOneLevelDir(composePath(parentDirName, childDirName))
        else createMultiLevelDir(parentDirName, childDirName)
    }


    @Throws(
        IOException::class,
        CloudWriter.UnsuccessfulResponseException::class,
        CloudWriter.AlreadyExistsException::class
    )
    private fun createMultiLevelDir(parentDirName: String, childDirName: String) {

        if (fileExists(parentDirName, childDirName))
            throw CloudWriter.AlreadyExistsException(childDirName)

        var pathToCreate = ""

        childDirName.split(CloudWriter.DS).forEach { dirName ->

            pathToCreate += CloudWriter.DS + dirName

            try {
                createOneLevelDir(parentDirName, pathToCreate)
            } catch (e: CloudWriter.AlreadyExistsException) {
                Log.d(TAG, "Dir '$pathToCreate' already exists.")
            }
        }
    }


    @Throws(
        IOException::class,
        CloudWriter.UnsuccessfulResponseException::class,
        CloudWriter.AlreadyExistsException::class
    )
    private fun createOneLevelDir(parentDirName: String, childDirName: String) {
        createOneLevelDir(composePath(parentDirName, childDirName))
    }

    @Throws(
        IOException::class,
        CloudWriter.UnsuccessfulResponseException::class,
        CloudWriter.AlreadyExistsException::class
    )
    private fun createOneLevelDir(absoluteDirPath: String) {

        val url = BASE_URL.toHttpUrl().newBuilder().apply {
            addQueryParameter("path", absoluteDirPath)
        }.build()

        val requestBody = "".toRequestBody(null)

        val request: Request = Request.Builder()
            .header("Authorization", authToken)
            .url(url)
            .put(requestBody)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            when (response.code) {
                201 -> return
                409 -> throw alreadyExistsException(absoluteDirPath)
                else -> throw unsuccessfulResponseException(response)
            }
        }
    }

    @Throws(
        IOException::class,
        CloudWriter.UnsuccessfulResponseException::class
    )
    override fun putFile(file: File, targetDirPath: String, overwriteIfExists: Boolean) {

        val fullTargetPath = "${targetDirPath}/${file.name}".stripExtraSlashes()

        val uploadURL = getURLForUpload(fullTargetPath, overwriteIfExists)

        putFileReal(file, uploadURL)
    }


    @Throws(IOException::class, CloudWriter.UnsuccessfulResponseException::class)
    override fun fileExists(parentDirName: String, childName: String): Boolean {

        val dirName = composePath(parentDirName, childName)

        val url = BASE_URL.toHttpUrl().newBuilder().apply {
            addQueryParameter("path", dirName)
        }.build()

        val request: Request = Request.Builder()
            .header("Authorization", authToken)
            .url(url)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            return when (response.code) {
                200 -> true
                404 -> false
                else -> throw unsuccessfulResponseException(response)
            }
        }
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


    private fun unsuccessfulResponseException(response: Response): Throwable
        = CloudWriter.UnsuccessfulResponseException(response.code, response.message)

    private fun alreadyExistsException(dirName: String): CloudWriter.AlreadyExistsException
            = CloudWriter.AlreadyExistsException(dirName)


    companion object {
        val TAG: String = YandexCloudWriter::class.java.simpleName
        private const val BASE_URL = "https://cloud-api.yandex.net/v1/disk/resources"
        private const val DEFAULT_MEDIA_TYPE = "application/octet-stream"
    }


}
