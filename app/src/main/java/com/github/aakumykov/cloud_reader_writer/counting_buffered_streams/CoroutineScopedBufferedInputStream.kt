package com.github.aakumykov.kotlin_playground.counting_buffered_streams

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import java.io.InputStream

class CoroutineScopedBufferedInputStream(
    private val inputStream: InputStream,
    private val coroutineScope: CoroutineScope,
    readingCallback: ReadingCallback,
)
    : CountingBufferedInputStream(
        inputStream = inputStream,
        readingCallback = readingCallback,
    )
{
    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        if (coroutineScope.isActive) {
            return super.read(b, off, len)
        } else {
            inputStream.close()
            throw CancellationException()
        }
    }
}