package com.github.aakumykov.cloud_writer

import java.io.File
import java.io.IOException

interface CloudWriter {

    @Throws(
        IOException::class,
        UnsuccessfulOperationException::class,
        AlreadyExistsException::class
    )
    fun createDir(basePath: String, dirName: String)


    // TODO: AlreadyExistsException
    @Throws(IOException::class, UnsuccessfulOperationException::class)
    fun putFile(file: File, targetDirPath: String, overwriteIfExists: Boolean = false)


    // TODO: не нужна
    // FIXME: добавить аннотацию в реализации
    @Throws(IOException::class, UnsuccessfulOperationException::class)
    fun fileExists(parentDirName: String, childName: String): Boolean


    sealed class CloudWriterException(message: String) : Exception(message)

    class UnsuccessfulOperationException(errorMsg: String) : CloudWriterException(errorMsg) {
        constructor(responseCode:Int, responseMessage: String) : this("$responseCode: $responseMessage")
    }

    class AlreadyExistsException(dirName: String)
        : CloudWriterException(dirName)


    companion object {
        const val DS = "/"
        const val ARG_NAME_AUTH_TOKEN = "AUTH_TOKEN_ARG"
    }
}