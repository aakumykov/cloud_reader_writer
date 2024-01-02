package com.github.aakumykov.cloud_writer

import java.io.File
import java.io.IOException

interface CloudWriter {

    @Throws(IOException::class, UnsuccessfulResponseException::class)
    fun createDir(dirName: String)


    @Throws(IOException::class, UnsuccessfulResponseException::class)
    fun createDirWithParents(dirName: String)


    @Throws(IOException::class, UnsuccessfulResponseException::class)
    fun putFile(file: File, targetDirPath: String, overwriteIfExists: Boolean = false)



    sealed class CloudWriterException(responseCode: Int, responseMessage: String)
        : Exception("${responseCode}: $responseMessage")


    class UnsuccessfulResponseException(responseCode: Int, responseMessage: String)
        : CloudWriterException(responseCode, responseMessage)


}