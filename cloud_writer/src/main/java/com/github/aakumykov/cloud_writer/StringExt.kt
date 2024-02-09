package com.github.aakumykov.cloud_writer

fun String.stripExtraSlashes(): String = this.replace(Regex("[/]+"),"/")