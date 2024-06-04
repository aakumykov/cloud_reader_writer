package com.github.aakumykov.cloud_reader_writer

import kotlinx.coroutines.flow.callbackFlow
import java.io.BufferedInputStream
import java.io.InputStream

class CountingInputStream(inputStream: InputStream,
                          private val callback: Callback,
                          bufferSize: Int = 8192) : BufferedInputStream(inputStream, bufferSize)
{
    private var _count: Int = 0
    val count: Int = _count

    override fun read(): Int {
        return super.read()
    }

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        val bytesRead = super.read(b, off, len)
        if (bytesRead > 0) {
            _count += bytesRead
            callback.onCountChanged(count, 0)
        }
        return bytesRead
    }

    interface Callback {
        fun onCountChanged(bytes: Int, total: Int)
    }
}
