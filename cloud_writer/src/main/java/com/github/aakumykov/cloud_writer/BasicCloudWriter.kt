package com.github.aakumykov.cloud_writer

abstract class BasicCloudWriter : CloudWriter {

    // TODO: избавиться от этого базового класса
    protected fun composePath(parentDirName: String, childDirName: String): String
            = "${parentDirName}${CloudWriter.DS}${childDirName}".stripMultiSlash()
}

private fun String.stripMultiSlash(): String {
    return this.replace(Regex("/+"),"/")
}