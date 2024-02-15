package com.github.aakumykov.cloud_writer

import com.github.aakumykov.cloud_writer.CloudWriter.Companion.ARG_NAME_AUTH_TOKEN
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.IOException

class LocalCloudWriter @AssistedInject constructor(
    @Assisted(ARG_NAME_AUTH_TOKEN) private val authToken: String
): BasicCloudWriter()
{
    @Throws(
        IOException::class,
        CloudWriter.UnsuccessfulResponseException::class,
        CloudWriter.AlreadyExistsException::class
    )
    override fun createDir(basePath: String, dirName: String) {

        val fullDirName = composePath(basePath, dirName)

        with(File(fullDirName)) {
            if (exists())
                throw CloudWriter.AlreadyExistsException(dirName)

            if (!mkdirs())
                throw CloudWriter.UnsuccessfulResponseException(0, dirNotCreatedMessage(basePath, dirName))
        }
    }


    @Throws(IOException::class, CloudWriter.UnsuccessfulResponseException::class)
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


    private fun dirNotCreatedMessage(parentDirName: String, childDirName: String): String
            = "Directory '${parentDirName}${CloudWriter.DS}${childDirName}' not created."
}