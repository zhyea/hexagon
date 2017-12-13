package hexagon.utils

import java.io.EOFException
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

object IOUtils {


  /**
    * 从Channel中读取数据到指定的ByteBuffer中，并返回读取到的字节数。
    * 如果channel已经关闭，或因为其他原因收到的返回值为-1，则抛出EOFException。
    */
  def read(channel: ReadableByteChannel, buffer: ByteBuffer): Int = {
    channel.read(buffer) match {
      case -1 => throw new EOFException("Received -1 when reading from channel, socket has likely been closed.")
      case n: Int => n
    }
  }


}
