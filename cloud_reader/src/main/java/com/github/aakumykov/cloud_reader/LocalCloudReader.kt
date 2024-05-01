package com.github.aakumykov.cloud_reader

import java.io.File
import java.io.FileInputStream

class LocalCloudReader : CloudReader {

    override suspend fun getFileInputStream(absolutePath: String): Result<FileInputStream> {
        return try {
            return Result.success(File(absolutePath).inputStream())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}