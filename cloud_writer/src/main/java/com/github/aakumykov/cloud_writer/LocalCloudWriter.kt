package com.github.aakumykov.cloud_writer

import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.IOException

class LocalCloudWriter @AssistedInject constructor(@Assisted authToken: String): CloudWriter {

    @Throws(IOException::class, CloudWriter.UnsuccessfulResponseException::class)
    override fun createDir(dirName: String) {
        File(dirName).mkdir()
    }

    override fun createDirWithParents(dirName: String) {
        TODO("Not yet implemented")
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