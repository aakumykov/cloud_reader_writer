package com.github.aakumykov.cloud_writer

import java.io.File
import java.io.IOException

interface CloudWriter {

    @Throws(IOException::class, UnsuccessfulResponseException::class, AlreadyExistsException::class)
    fun createDir(parentDirName: String, childDirName: String)


    // TODO: AlreadyExistsException
    @Throws(IOException::class, UnsuccessfulResponseException::class)
    fun putFile(file: File, targetDirPath: String, overwriteIfExists: Boolean = false)


    // TODO: не нужна
    @Throws(IOException::class, UnsuccessfulResponseException::class)
    fun dirExists(parentDirName: String, childDirName: String): Boolean



    sealed class CloudWriterException(message: String)
        : Exception(message)

    class UnsuccessfulResponseException(responseCode: Int, responseMessage: String)
        : CloudWriterException("$responseCode: $responseMessage")

    class AlreadyExistsException(dirName: String)
        : CloudWriterException(dirName)


    companion object {
        const val DS = "/"
        const val CLOUD_ROOT_DIR = "/"
        const val ARG_NAME_AUTH_TOKEN = "AUTH_TOKEN_ARG"
        const val ARG_NAME_ROOT_DIR = "ROOT_DIR_ARG"
    }
}