package com.github.aakumykov.cloud_reader.local_cloud_reader

import com.github.aakumykov.cloud_reader.CloudReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

class LocalCloudReader : CloudReader {

    override suspend fun getDownloadLink(absolutePath: String): Result<String> {
        return if (fileExistsSimple(absolutePath)) Result.success(absolutePath)
        else Result.failure(FileNotFoundException("File not found '$absolutePath'"))
    }

    override suspend fun getFileInputStream(absolutePath: String): Result<FileInputStream> {
        return try {
            return Result.success(File(absolutePath).inputStream())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fileExists(absolutePath: String): Result<Boolean> {
        return try {
            Result.success(fileExistsSimple(absolutePath))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun fileExistsSimple(absolutePath: String): Boolean = File(absolutePath).exists()
}