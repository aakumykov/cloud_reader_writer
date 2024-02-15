package com.github.aakumykov.cloud_writer

import com.github.aakumykov.cloud_writer.CloudWriter.Companion.ARG_NAME_AUTH_TOKEN
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class LocalCloudWriter @AssistedInject constructor(
    @Assisted(ARG_NAME_AUTH_TOKEN) private val authToken: String
): CloudWriter
{
    @Throws(
        IOException::class,
        CloudWriter.UnsuccessfulOperationException::class,
        CloudWriter.AlreadyExistsException::class
    )
    override fun createDir(basePath: String, dirName: String) {

        val fullDirName = CloudWriter.composeFullPath(basePath, dirName)

        with(File(fullDirName)) {
            if (exists())
                throw CloudWriter.AlreadyExistsException(dirName)

            if (!mkdirs())
                throw CloudWriter.UnsuccessfulOperationException(0, dirNotCreatedMessage(basePath, dirName))
        }
    }


    @Throws(IOException::class, CloudWriter.UnsuccessfulOperationException::class)
    override fun putFile(file: File, targetDirPath: String, overwriteIfExists: Boolean) {

        val fullTargetPath = "${targetDirPath}/${file.name}".stripExtraSlashes()
        val targetFile = File(fullTargetPath)

        val isMoved = file.renameTo(targetFile)

        if (!isMoved)
            throw IOException("File cannot be not moved from '${file.absolutePath}' to '${fullTargetPath}'")
    }

    
    override fun fileExists(parentDirName: String, childName: String): Boolean {
        return File(parentDirName, childName).exists()
    }

    @Throws(IOException::class, CloudWriter.UnsuccessfulOperationException::class)
    override fun deleteFile(basePath: String, fileName: String) {

        val path = CloudWriter.composeFullPath(basePath, fileName)

        with(File(path)) {
            if (!exists())
                throw FileNotFoundException(path)

            if (!delete())
                throw UnsupportedOperationException("File '$path' was not deleted.")
        }
    }


    private fun dirNotCreatedMessage(parentDirName: String, childDirName: String): String
            = "Directory '${parentDirName}${CloudWriter.DS}${childDirName}' not created."
}