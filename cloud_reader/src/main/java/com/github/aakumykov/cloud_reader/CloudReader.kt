package com.github.aakumykov.cloud_reader

import java.io.InputStream

// TODO: переименовать в StorageReader
interface CloudReader {
    // FIXME: разобраться с "?"
    suspend fun getDownloadLink(absolutePath: String): Result<String?>
    suspend fun getFileInputStream(absolutePath: String): Result<InputStream?>
}