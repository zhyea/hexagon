package hexagon.utils

import java.io.{File, FileInputStream, RandomAccessFile}
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.{Charset, StandardCharsets}

import hexagon.exceptions.HexagonException

object IOUtils {


  /**
    * 读取字符串（字符串长度+字符串数据），字符串长度小于Short.MAX
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
    * 写入字符串（字符串长度+字符串数据），字符串长度小于Short.MAX
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

  /**
    * 读取字符串数据长度（字符串长度+字符串数据），字符串长度小于Short.MAX
    */
  def shortStringLength(string: String): Int = {
    if (null == string) {
      java.lang.Short.BYTES
    } else {
      val encodedString = string.getBytes(StandardCharsets.UTF_8)
      if (encodedString.length > Short.MaxValue) {
        throw new HexagonException(s"String exceeds the maximum size of ${Short.MaxValue}.")
      } else {
        java.lang.Short.BYTES + encodedString.length
      }
    }
  }


  def openChannel(file: File, mutable: Boolean): FileChannel = {
    if (mutable) {
      new RandomAccessFile(file, "rw").getChannel
    } else {
      new FileInputStream(file).getChannel
    }
  }


}
