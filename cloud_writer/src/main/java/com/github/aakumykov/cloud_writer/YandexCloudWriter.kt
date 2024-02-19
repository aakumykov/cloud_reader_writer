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
import java.util.concurrent.TimeUnit

class YandexCloudWriter @AssistedInject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    @Assisted private val authToken: String
) : CloudWriter
{
    // TODO: проверить с разными аргументами
    @Throws(
        IOException::class,
        CloudWriter.OperationUnsuccessfulException::class,
        CloudWriter.AlreadyExistsException::class
    )
    override fun createDir(basePath: String, dirName: String) {
        if (!dirName.contains(CloudWriter.DS)) createOneLevelDir(CloudWriter.composeFullPath(basePath, dirName))
        else createMultiLevelDir(basePath, dirName)
    }


    @Throws(
        IOException::class,
        CloudWriter.OperationUnsuccessfulException::class,
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
        CloudWriter.OperationUnsuccessfulException::class,
        CloudWriter.AlreadyExistsException::class
    )
    private fun createOneLevelDir(parentDirName: String, childDirName: String) {
        createOneLevelDir(CloudWriter.composeFullPath(parentDirName, childDirName))
    }

    @Throws(
        IOException::class,
        CloudWriter.OperationUnsuccessfulException::class,
        CloudWriter.AlreadyExistsException::class
    )
    private fun createOneLevelDir(absoluteDirPath: String) {

        val url = RESOURCES_BASE_URL.toHttpUrl().newBuilder().apply {
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
        CloudWriter.OperationUnsuccessfulException::class
    )
    override fun putFile(file: File, targetDirPath: String, overwriteIfExists: Boolean) {

        val fullTargetPath = CloudWriter.composeFullPath(targetDirPath, file.name)

        val uploadURL = getURLForUpload(fullTargetPath, overwriteIfExists)

        putFileReal(file, uploadURL)
    }


    @Throws(IOException::class, CloudWriter.OperationUnsuccessfulException::class)
    override fun fileExists(parentDirName: String, childName: String): Boolean {

        val dirName = CloudWriter.composeFullPath(parentDirName, childName)

        val url = RESOURCES_BASE_URL.toHttpUrl().newBuilder().apply {
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


    @Throws(
        IOException::class,
        CloudWriter.OperationUnsuccessfulException::class,
        CloudWriter.OperationTimeoutException::class
    )
    override fun deleteFile(basePath: String, fileName: String) {

        var timeElapsed = 0L

        try {
            deleteFileSimple(basePath, fileName)
        }
        catch (e: IndeterminateOperationException) {
            while(!operationIsFinished(e.operationStatusLink)) {

                TimeUnit.MILLISECONDS.sleep(OPERATION_WAITING_STEP_MILLIS)

                timeElapsed += OPERATION_WAITING_STEP_MILLIS

                if (timeElapsed > OPERATION_WAITING_TIMEOUT_MILLIS)
                    throw CloudWriter.OperationTimeoutException("Deletion of file '${CloudWriter.composeFullPath(basePath, fileName)}' is timed out. Maybe it is really deleted.")
            }
        }
    }

    @Throws(CloudWriter.OperationUnsuccessfulException::class)
    private fun operationIsFinished(operationStatusLink: String): Boolean {
        val request = Request.Builder()
            .url(operationStatusLink)
            .header("Authorization", authToken)
            .get()
            .build()
        return okHttpClient.newCall(request).execute().use { response ->
            when(response.code) {
                200 -> statusResponseToBoolean(response)
                else -> throw CloudWriter.OperationUnsuccessfulException(response.code, response.message)
            }
        }
    }

    private fun statusResponseToBoolean(response: Response): Boolean {
        val operationStatus = gson.fromJson(response.body?.string(), OperationStatus::class.java)
        return "success" == operationStatus.status
    }

    @Throws(IndeterminateOperationException::class)
    private fun deleteFileSimple(basePath: String, fileName: String) {

        val url = RESOURCES_BASE_URL.toHttpUrl().newBuilder().apply {
            addQueryParameter("path", fileName)
        }.build()

        val request: Request = Request.Builder()
            .header("Authorization", authToken)
            .url(url)
            .delete()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            when (response.code) {
                204 -> return
                202 -> throw IndeterminateOperationException(linkFromResponse(response))
                else -> throw unsuccessfulResponseException(response)
            }
        }
    }


    @Throws(IOException::class, CloudWriter.OperationUnsuccessfulException::class)
    private fun getURLForUpload(targetFilePath: String, overwriteIfExists: Boolean): String {


        val url = UPLOAD_BASE_URL.toHttpUrl().newBuilder()
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
                return linkFromResponse(response)
            else
                throw unsuccessfulResponseException(response)
        }
    }


    @Throws(IOException::class, CloudWriter.OperationUnsuccessfulException::class)
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
        = CloudWriter.OperationUnsuccessfulException(response.code, response.message)

    private fun alreadyExistsException(dirName: String): CloudWriter.AlreadyExistsException
            = CloudWriter.AlreadyExistsException(dirName)



    private fun linkFromResponse(response: Response): String {
        return gson.fromJson(response.body?.string(), Link::class.java).href
    }


    companion object {
        val TAG: String = YandexCloudWriter::class.java.simpleName

        const val OPERATION_WAITING_STEP_MILLIS = 100L
        const val OPERATION_WAITING_TIMEOUT_MILLIS = 30_000L

        private const val DISK_BASE_URL = "https://cloud-api.yandex.net/v1/disk"
        private const val RESOURCES_BASE_URL = "${DISK_BASE_URL}/resources"
        private const val UPLOAD_BASE_URL = "$RESOURCES_BASE_URL/upload"

        private const val DEFAULT_MEDIA_TYPE = "application/octet-stream"
    }

    class IndeterminateOperationException(val operationStatusLink: String) : Exception()

    inner class OperationStatus(val status: String)
}
