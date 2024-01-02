package com.github.aakumykov.cloud_writer

import com.github.aakumykov.cloud_writer.CloudWriter.Companion.ARG_NAME_AUTH_TOKEN
import com.github.aakumykov.cloud_writer.CloudWriter.Companion.ARG_NAME_ROOT_DIR
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.IOException

class LocalCloudWriter @AssistedInject constructor(
    @Assisted(ARG_NAME_ROOT_DIR) private val rootDir: String,
    @Assisted(ARG_NAME_AUTH_TOKEN) private val authToken: String
): BasicCloudWriter()
{
    @Throws(IOException::class)
    override fun createDir(dirName: String) {
        File(dirName).mkdir()
    }


    @Throws(IOException::class)
    override fun createDirWithParents(dirName: String) {
        with(dirName) {
            this.trim()
            iterateThroughDirHierarchy(rootDir + CloudWriter.DS + this) { nextDirName ->
                File(nextDirName).mkdirs()
            }
        }
    }


    @Throws(IOException::class, CloudWriter.UnsuccessfulResponseException::class)
    override fun putFile(file: File, targetDirPath: String, overwriteIfExists: Boolean) {

        val fullTargetPath = "${targetDirPath}/${file.name}".replace(Regex("[/]+"),"/")
        val targetFile = File(fullTargetPath)

        val isMoved = file.renameTo(targetFile)

        if (!isMoved)
            throw IOException("File cannot be not moved from '${file.absolutePath}' to '${fullTargetPath}'")
    }

}