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
    @Throws(
        IOException::class,
        CloudWriter.UnsuccessfulResponseException::class,
        CloudWriter.AlreadyExistsException::class
    )
    override fun createSimpleDir(parentDirName: String, childDirName: String) {

//        if (childDirName.contains(CloudWriter.DS))
//            throw IllegalArgumentException("childDirName parameter cannot contains dir separators, it must be single-level directory name.")
//
//        if (!dirExists(CloudWriter.CLOUD_ROOT_DIR,parentDirName))
//            throw IllegalArgumentException("Parent dir must exists on server; you should use createDeepDir() method to create dir and its parent dirs.")

        val fullDirName = composePath(parentDirName, childDirName)

        val url = BASE_URL.toHttpUrl().newBuilder().apply {
            addQueryParameter("path", fullDirName)
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
                409 -> throw alreadyExistsException(fullDirName)
                else -> throw unsuccessfulResponseException(response)
            }
        }
    }


    // TODO: игнорировать, если существует
    @Throws(
        IOException::class,
        CloudWriter.UnsuccessfulResponseException::class,
        CloudWriter.AlreadyExistsException::class
    )
    override fun createDeepDir(parentDirName: String, childDirName: String) {

        if (dirExists(parentDirName, childDirName))
            throw CloudWriter.AlreadyExistsException("${parentDirName}${CloudWriter.DS}${childDirName}")

        with(childDirName) {
            this.trim()
            iterateThroughDirHierarchy(this) { nextDirName ->
                try {
                    createSimpleDir(parentDirName, nextDirName)
                } catch (e: CloudWriter.AlreadyExistsException) {
                    Log.d(TAG, "Каталог '${composePath(parentDirName, childDirName)}' уже существует.")
                }
            }
        }
    }


    @Throws(
        IOException::class,
        CloudWriter.UnsuccessfulResponseException::class
    )
    override fun putFile(file: File, targetDirPath: String, overwriteIfExists: Boolean) {

        val fullTargetPath = "${targetDirPath}/${file.name}".replace(Regex("[/]+"),"/")

        val uploadURL = getURLForUpload(fullTargetPath, overwriteIfExists)

        putFileReal(file, uploadURL)
    }


    @Throws(IOException::class, CloudWriter.UnsuccessfulResponseException::class)
    override fun dirExists(parentDirName: String, childDirName: String): Boolean {

        val dirName = composePath(parentDirName, childDirName)

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
