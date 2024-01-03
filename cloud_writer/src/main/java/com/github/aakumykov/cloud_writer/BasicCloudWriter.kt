package com.github.aakumykov.cloud_writer

abstract class BasicCloudWriter : CloudWriter {

    fun iterateThroughDirHierarchy(dirName: String, onNextDirName: (nextDirName: String) -> Unit) {
        with(dirName) {
            restoreStartingSlash(this)
            this.split(DS).reduce { nextDirPath, nextDirName ->
                val dirPath = "$nextDirPath/$nextDirName"
                onNextDirName(dirPath)
                dirPath
            }
        }
    }

    private fun restoreStartingSlash(dirName: String): String {
        return dirName
            .replace(Regex("^"), DS)
            .replace(Regex("^[${DS}]+"), DS)
    }


    @Deprecated("Следить вручную...")
    fun deduplicateDirSeparators(dirName: String): String  = dirName.replace(Regex("[/]+"), "/")

    companion object {
        private const val DS = "/"
    }
}
