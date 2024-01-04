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
    /*@Throws(
        IOException::class,
        CloudWriter.UnsuccessfulResponseException::class
    )
    override fun createSimpleDir(parentDirName: String, childDirName: String) {
        val fullDirName = composePath(parentDirName, childDirName)
        if (!File(fullDirName).mkdir())
            throw CloudWriter.UnsuccessfulResponseException(0, dirNotCreatedMessage(parentDirName, childDirName))
    }*/


    @Throws(
        IOException::class,
        CloudWriter.UnsuccessfulResponseException::class
    )
    override fun createDir(parentDirName: String, childDirName: String) {
        val fullDirName = composePath(parentDirName, childDirName)
        if (!File(fullDirName).mkdirs())
            throw CloudWriter.UnsuccessfulResponseException(0, dirNotCreatedMessage(parentDirName, childDirName))
    }


    @Throws(IOException::class, CloudWriter.UnsuccessfulResponseException::class)
    override fun putFile(file: File, targetDirPath: String, overwriteIfExists: Boolean) {

        val fullTargetPath = "${targetDirPath}/${file.name}".replace(Regex("[/]+"),"/")
        val targetFile = File(fullTargetPath)

        val isMoved = file.renameTo(targetFile)

        if (!isMoved)
            throw IOException("File cannot be not moved from '${file.absolutePath}' to '${fullTargetPath}'")
    }

    override fun dirExists(parentDirName: String, childDirName: String): Boolean {
        return File(parentDirName, childDirName).exists()
    }


    private fun dirNotCreatedMessage(parentDirName: String, childDirName: String): String
            = "Directory '${parentDirName}${CloudWriter.DS}${childDirName}' not created."
}