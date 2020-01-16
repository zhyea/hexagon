package hexagon.protocol

import java.io.InputStream
import java.nio.ByteBuffer

class ByteBufferBackedInputStream(buffer: ByteBuffer) extends InputStream {

  override def read(): Int = if (buffer.hasRemaining) buffer.get() & 0xFF else -1

  override def read(bytes: Array[Byte], off: Int, len: Int): Int =
    buffer.hasRemaining match {
      case true =>
        val lenRead = Math.min(len, buffer.remaining())
        buffer.get(bytes, off, lenRead)
        lenRead

      case false => -1
    }
}