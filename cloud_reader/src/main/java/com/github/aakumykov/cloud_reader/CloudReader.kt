package com.github.aakumykov.cloud_reader

import java.io.InputStream

// TODO: переименовать в StorageReader
interface CloudReader {
    suspend fun getFileInputStream(absolutePath: String): Result<InputStream>
}