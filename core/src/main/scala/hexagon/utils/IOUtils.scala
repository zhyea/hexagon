package hexagon.utils

import java.nio.ByteBuffer
import java.nio.charset.{Charset, StandardCharsets}

object IOUtils {


  /**
    * Read size prefixed string where the size is stored as a 2 byte short.
    */
  def readShortString(buffer: ByteBuffer, encoding: Charset = StandardCharsets.UTF_8): String = {
    val size: Int = buffer.getShort()
    if (size < 0)
      return null
    val bytes = new Array[Byte](size)
    buffer.get(bytes)
    new String(bytes, encoding)
  }

  /**
    * Write a size prefixed string where the size is stored as a 2 byte short
    */
  def writeShortString(buffer: ByteBuffer, str: String, encoding: Charset = StandardCharsets.UTF_8): Unit = {
    if (str == null) {
      buffer.putShort(-1)
    } else if (str.length > Short.MaxValue) {
      throw new IllegalArgumentException("String exceeds the maximum size of " + Short.MaxValue + ".")
    } else {
      buffer.putShort(str.length.asInstanceOf[Short])
      buffer.put(str.getBytes(encoding))
    }
  }
}
