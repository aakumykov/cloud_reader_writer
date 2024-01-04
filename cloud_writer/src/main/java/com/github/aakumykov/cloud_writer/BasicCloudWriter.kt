package com.github.aakumykov.cloud_writer

abstract class BasicCloudWriter : CloudWriter {

    /*fun iterateThroughDirHierarchy(dirName: String, onNextDirName: (nextDirName: String) -> Unit) {
        with(dirName) {
            this.split(DS).reduce { nextDirPath, nextDirName ->
                val dirPath = "$nextDirPath/$nextDirName"
                onNextDirName(nextDirPath)
                dirPath
            }
        }
    }*/

    private fun restoreStartingSlash(dirName: String): String {
        return (DS + dirName).replace(Regex("^[${DS}]+"), DS)
    }


    protected fun composePath(parentDirName: String, childDirName: String): String
            = "${parentDirName}${CloudWriter.DS}${childDirName}".stripMultiSlash()


    companion object {
        private const val DS = "/"
    }
}

private fun String.stripMultiSlash(): String {
    return this.replace(Regex("/+"),"/")
}