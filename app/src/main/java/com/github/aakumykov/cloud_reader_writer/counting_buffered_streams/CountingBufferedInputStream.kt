package com.github.aakumykov.kotlin_playground.counting_buffered_streams

import java.io.BufferedInputStream
import java.io.InputStream

open class CountingBufferedInputStream(
    private val inputStream: InputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    private val readingCallback: ReadingCallback,
)
    : BufferedInputStream(inputStream, bufferSize)
{
    private var readedBytesCount: Long = 0


    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        return super.read(b, off, len).let { count ->
            summarizeAndCallBack(count)
            count
        }
    }


    override fun close() {
        super.close()
        inputStream.close()
    }

    private fun summarizeAndCallBack(count: Int) {

        if (-1 == count) {
            invokeCallback()
            return
        }

        readedBytesCount += count

        invokeCallback()
    }

    private fun invokeCallback() {
        readingCallback.onReadCountChanged(readedBytesCount)
    }


    fun interface ReadingCallback {
        fun onReadCountChanged(count: Long)
    }


    companion object {
        const val DEFAULT_BUFFER_SIZE = 8192
    }
}