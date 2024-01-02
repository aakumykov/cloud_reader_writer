package com.github.aakumykov.cloud_writer

import java.io.File
import java.io.IOException

interface CloudWriter {

    // TODO: игнорировать, если существует
    @Throws(IOException::class, UnsuccessfulResponseException::class, AlreadyExistsException::class)
    fun createDir(parentDirName: String, childDirName: String)


    @Throws(IOException::class, UnsuccessfulResponseException::class, AlreadyExistsException::class)
    fun createDirWithParents(parentDirName: String, childDirName: String)


    // TODO: AlreadyExistsException
    @Throws(IOException::class, UnsuccessfulResponseException::class)
    fun putFile(file: File, targetDirPath: String, overwriteIfExists: Boolean = false)



    sealed class CloudWriterException(responseCode: Int, responseMessage: String)
        : Exception("${responseCode}: $responseMessage")


    open class UnsuccessfulResponseException(responseCode: Int, responseMessage: String)
        : CloudWriterException(responseCode, responseMessage)


    class AlreadyExistsException(responseCode: Int, responseMessage: String)
        : CloudWriterException(responseCode, responseMessage)


    companion object {
        const val DS = "/"
        const val ARG_NAME_AUTH_TOKEN = "AUTH_TOKEN_ARG"
        const val ARG_NAME_ROOT_DIR = "ROOT_DIR_ARG"
    }
}