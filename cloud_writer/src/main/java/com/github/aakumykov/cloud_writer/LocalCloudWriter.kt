package com.github.aakumykov.cloud_writer

import com.github.aakumykov.cloud_writer.CloudWriter.OperationTimeoutException
import com.github.aakumykov.cloud_writer.CloudWriter.OperationUnsuccessfulException
import com.github.aakumykov.cloud_writer.extensions.copyTo
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class LocalCloudWriter constructor(
    private val authToken: String
): CloudWriter
{
    @Throws(
        IOException::class,
        CloudWriter.OperationUnsuccessfulException::class,
        CloudWriter.AlreadyExistsException::class
    )
    override fun createDir(basePath: String, dirName: String) {

        val fullDirName = CloudWriter.composeFullPath(basePath, dirName)

        with(File(fullDirName)) {
            if (!exists())
                if (!mkdirs())
                    throw CloudWriter.OperationUnsuccessfulException(0, dirNotCreatedMessage(absolutePath))
        }
    }


    @Throws(IOException::class, CloudWriter.OperationUnsuccessfulException::class)
    override fun putFile(file: File, targetPath: String, overwriteIfExists: Boolean) {

        val targetFile = File(targetPath)

        val isMoved = file.renameTo(targetFile)

        if (!isMoved)
            throw IOException("File cannot be not moved from '${file.absolutePath}' to '${targetPath}'")
    }


    @Throws(IOException::class, CloudWriter.OperationUnsuccessfulException::class)
    override fun putFile(inputStream: InputStream, targetPath: String, overwriteIfExists: Boolean) {

        val targetFile = File(targetPath)
        if (targetFile.exists() && !overwriteIfExists)
            return

        File(targetPath).outputStream().use { os ->
            inputStream.copyTo(os)
        }
    }


    override fun fileExists(parentDirName: String, childName: String): Boolean {
        return File(parentDirName, childName).exists()
    }

    @Throws(
        IOException::class,
        CloudWriter.OperationUnsuccessfulException::class,
        CloudWriter.OperationTimeoutException::class
    )
    override fun deleteFile(basePath: String, fileName: String) {

        val path = CloudWriter.composeFullPath(basePath, fileName)

        with(File(path)) {
            if (!exists())
                throw FileNotFoundException(path) // FIXME: осмысленное сообщение

            if (!delete())
                throw UnsupportedOperationException("File '$path' was not deleted.") // FIXME: OperationUnsuccessfulException
        }
    }

    @Throws(
        IOException::class,
        OperationUnsuccessfulException::class,
        OperationTimeoutException::class
    )
    override fun deleteDirRecursively(basePath: String, dirName: String) {

        val path = CloudWriter.composeFullPath(basePath, dirName)

        with(File(path)) {
            if (!exists())
                throw FileNotFoundException("Dir not found: $path")

            if (!deleteRecursively())
                throw OperationUnsuccessfulException("Dir was not deleted recursively: $path")
        }
    }


    private fun dirNotCreatedMessage(dirName: String): String
            = "Directory '${dirName}' not created."
}