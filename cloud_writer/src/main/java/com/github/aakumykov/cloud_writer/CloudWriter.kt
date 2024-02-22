package com.github.aakumykov.cloud_writer

import java.io.File
import java.io.IOException

interface CloudWriter {

    @Throws(
        IOException::class,
        OperationUnsuccessfulException::class,
        AlreadyExistsException::class
    )
    fun createDir(basePath: String, dirName: String)


    // TODO: AlreadyExistsException
    /**
     * Отправляет файл по указанному пути.
     * Реализации обязаны использовать параметр targetPath "как есть", не внося в него корректировки!
     */
    @Throws(IOException::class, OperationUnsuccessfulException::class)
    fun putFile(file: File, targetPath: String, overwriteIfExists: Boolean = false)


    // TODO: не нужна
    // FIXME: добавить аннотацию в реализации
    @Throws(IOException::class, OperationUnsuccessfulException::class)
    fun fileExists(parentDirName: String, childName: String): Boolean


    // TODO: локальное удаление в корзину
    /**
     * Удаляет файл/папку.
     */
    @Throws(
        IOException::class,
        OperationUnsuccessfulException::class,
        OperationTimeoutException::class
    )
    fun deleteFile(basePath: String, fileName: String)

    // TODO: выделить в отдельный файл...
    sealed class CloudWriterException(message: String) : Exception(message)

    class OperationUnsuccessfulException(errorMsg: String) : CloudWriterException(errorMsg) {
        constructor(responseCode:Int, responseMessage: String) : this("$responseCode: $responseMessage")
    }

    class OperationTimeoutException(errorMsg: String) : CloudWriterException(errorMsg)

    class AlreadyExistsException(dirName: String) : CloudWriterException(dirName)


    companion object {
        const val DS = "/"
        const val ARG_NAME_AUTH_TOKEN = "AUTH_TOKEN_ARG"

        fun composeFullPath(basePath: String, fileName: String): String {
            return "${basePath}${DS}${fileName}".stripMultiSlashes()
        }
    }
}