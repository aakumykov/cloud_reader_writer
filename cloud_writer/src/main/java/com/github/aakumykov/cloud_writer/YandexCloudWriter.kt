package com.github.aakumykov.cloud_writer

import android.util.Log
import com.google.gson.Gson
import com.yandex.disk.rest.json.Link
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSink
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

class YandexCloudWriter(
    private val authToken: String,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().build(),
    private val gson: Gson = Gson()
) : CloudWriter {

    private val defaultMediaType: MediaType get() = DEFAULT_MEDIA_TYPE.toMediaType()

    // TODO: проверить с разными аргументами
    @Throws(
        IOException::class,
        CloudWriter.OperationUnsuccessfulException::class,
        CloudWriter.AlreadyExistsException::class
    )
    override fun createDir(basePath: String, dirName: String) {
        Log.d(TAG, "createDir() called with: basePath = $basePath, dirName = $dirName")
        if (!dirName.contains(CloudWriter.DS)) createOneLevelDir(CloudWriter.composeFullPath(basePath, dirName))
        else createMultiLevelDir(basePath, dirName)
    }


    @Throws(
        IOException::class,
        CloudWriter.OperationUnsuccessfulException::class,
        CloudWriter.AlreadyExistsException::class
    )
    private fun createMultiLevelDir(parentDirName: String, childDirName: String) {

        Log.d(TAG, "createMultiLevelDir() called with: parentDirName = $parentDirName, childDirName = $childDirName")

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
        Log.d(TAG, "createOneLevelDir() called with: parentDirName = $parentDirName, childDirName = $childDirName")
        createOneLevelDir(CloudWriter.composeFullPath(parentDirName, childDirName))
    }

    @Throws(
        IOException::class,
        CloudWriter.OperationUnsuccessfulException::class,
        CloudWriter.AlreadyExistsException::class
    )
    private fun createOneLevelDir(absoluteDirPath: String) {

        Log.d(TAG, "createOneLevelDir() called with: absoluteDirPath = $absoluteDirPath")

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


    @Throws(IOException::class, CloudWriter.OperationUnsuccessfulException::class)
    override fun putFile(file: File, targetPath: String, overwriteIfExists: Boolean) {

        Log.d(TAG, "putFile() called with: file = $file, targetDirPath = $targetPath, overwriteIfExists = $overwriteIfExists")

        val uploadURL = getURLForUpload(targetPath, overwriteIfExists)
        putFileReal(file, uploadURL)
    }


    @Throws(IOException::class, CloudWriter.OperationUnsuccessfulException::class)
    override fun putFile(inputStream: InputStream, targetPath: String, overwriteIfExists: Boolean) {
        val uploadURL = getURLForUpload(targetPath, overwriteIfExists)
        putFileAsStreamReal(inputStream, uploadURL)
    }


    @Throws(IOException::class, CloudWriter.OperationUnsuccessfulException::class)
    override fun fileExists(parentDirName: String, childName: String): Boolean {

        Log.d(
            TAG,
            "fileExists() called with: parentDirName = $parentDirName, childName = $childName"
        )

        val dirName = CloudWriter.composeFullPath(parentDirName, childName)

        Log.d(TAG, "dirName: $dirName")

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

        Log.d(TAG, "deleteFile() called with: basePath = $basePath, fileName = $fileName")

        var timeElapsed = 0L

        try {
            deleteFileSimple(basePath, fileName)
        }
        catch (e: IndeterminateOperationException) {
            Log.d(TAG, "Длительная операция удаления (${hashCode()}) ...")

            while(!operationIsFinished(e.operationStatusLink)) {

                Log.d(TAG, "... операция удаления продолжается (${hashCode()}) ...")

                TimeUnit.MILLISECONDS.sleep(OPERATION_WAITING_STEP_MILLIS)

                timeElapsed += OPERATION_WAITING_STEP_MILLIS

                if (timeElapsed > FILE_OPERATION_WAITING_TIMEOUT_MILLIS)
                    throw CloudWriter.OperationTimeoutException("Deletion of file '${CloudWriter.composeFullPath(basePath, fileName)}' is timed out. Maybe it is really deleted.")
            }

            Log.d(TAG, "... операция удаления завершена (${hashCode()}).")
        }
    }

    override fun deleteDirRecursively(basePath: String, dirName: String) {
        var timeElapsed = 0L
        try {
            deleteFileSimple(basePath, dirName)
        }
        catch (e: IndeterminateOperationException) {
            while(!operationIsFinished(e.operationStatusLink)) {
                TimeUnit.MILLISECONDS.sleep(OPERATION_WAITING_STEP_MILLIS)
                timeElapsed += OPERATION_WAITING_STEP_MILLIS
                if (timeElapsed > DIR_OPERATION_WAITING_TIMEOUT_MILLIS)
                    throw CloudWriter.OperationTimeoutException("Deletion of file '${CloudWriter.composeFullPath(basePath, dirName)}' is timed out. Maybe it is really deleted.")

            }
        }
    }

    @Throws(CloudWriter.OperationUnsuccessfulException::class)
    private fun operationIsFinished(operationStatusLink: String): Boolean {
        Log.d(TAG, "operationIsFinished() called with: operationStatusLink = $operationStatusLink")
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

        Log.d(TAG, "deleteFileSimple() called with: basePath = $basePath, fileName = $fileName")

        val path = CloudWriter.composeFullPath(basePath, fileName)

        val url = RESOURCES_BASE_URL.toHttpUrl().newBuilder().apply {
            addQueryParameter("path", path)
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

        Log.d(
            TAG,
            "getURLForUpload() called with: targetFilePath = $targetFilePath, overwriteIfExists = $overwriteIfExists"
        )

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

        Log.d(TAG, "putFileReal() called with: file = $file, uploadURL = $uploadURL")

        val requestBody: RequestBody = file.asRequestBody(defaultMediaType)

        performUploadRequest(requestBody, uploadURL)
    }

    @Throws(IOException::class, CloudWriter.OperationUnsuccessfulException::class)
    private fun putFileAsStreamReal(inputStream: InputStream, uploadURL: String) {

        val requestBody: RequestBody = object: RequestBody() {

            override fun contentType(): MediaType = defaultMediaType

            override fun writeTo(sink: BufferedSink) {
                inputStream.copyTo(sink.outputStream())
            }
        }

        performUploadRequest(requestBody, uploadURL)
    }


    private fun performUploadRequest(requestBody: RequestBody, uploadURL: String) {

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
        const val FILE_OPERATION_WAITING_TIMEOUT_MILLIS = 30_000L
        const val DIR_OPERATION_WAITING_TIMEOUT_MILLIS = 30_000L

        private const val DISK_BASE_URL = "https://cloud-api.yandex.net/v1/disk"
        private const val RESOURCES_BASE_URL = "${DISK_BASE_URL}/resources"
        private const val UPLOAD_BASE_URL = "$RESOURCES_BASE_URL/upload"

        private const val DEFAULT_MEDIA_TYPE = "application/octet-stream"
    }

    class IndeterminateOperationException(val operationStatusLink: String) : Exception()

    inner class OperationStatus(val status: String)
}
