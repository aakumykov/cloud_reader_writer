package com.github.aakumykov.cloud_reader_writer.fragments

import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.jvm.Throws

class CountingInputStream(
    private val inputStream: InputStream,
    bufferSize: Int = 8192,
    private val readCallback: ReadCallback
) : BufferedInputStream(inputStream, bufferSize) {

    private var readBytesCount: Long = 0

    @Throws(IOException::class)
    override fun read(): Int {
        return inputStream.read().let { justReadByte ->
            summarizeAndCallBack(1)
            justReadByte
        }
    }

    override fun read(b: ByteArray?): Int {
        return super.read(b).let { justReadCount ->
            summarizeAndCallBack(justReadCount)
            justReadCount
        }
    }

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        return super.read(b, off, len).let { justReadCount ->
            summarizeAndCallBack(justReadCount)
            justReadCount
        }
    }


    private fun summarizeAndCallBack(count: Int) {
        readBytesCount += count
        readCallback.onReadCountChanged(readBytesCount)
    }


    fun interface ReadCallback {
        fun onReadCountChanged(bytesRead: Long)
    }
}