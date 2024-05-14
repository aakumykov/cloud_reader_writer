package com.github.aakumykov.cloud_reader

import java.io.InputStream

// TODO: переименовать в StorageReader
interface CloudReader {
    suspend fun getDownloadLink(absolutePath: String): Result<String>
    suspend fun getFileInputStream(absolutePath: String): Result<InputStream>
    suspend fun fileExists(absolutePath: String): Result<Boolean>
}