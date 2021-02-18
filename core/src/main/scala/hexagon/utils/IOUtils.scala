package hexagon.utils

import io.vertx.core.buffer.Buffer

import java.io.{File, FileInputStream, RandomAccessFile}
import java.nio.channels.FileChannel
import java.nio.charset.{Charset, StandardCharsets}

object IOUtils {


	/**
	  * Read size prefixed string where the size is stored as a 2 byte short.
	  */
	def readShortString(buffer: Buffer, encoding: Charset = StandardCharsets.UTF_8): String = {
		val size: Int = buffer.getUnsignedShort(0)
		if (size < 0)
			return null
		val bytes = new Array[Byte](size)
		buffer.getBytes(bytes)
		new String(bytes, encoding)
	}

	/**
	  * Write a size prefixed string where the size is stored as a 2 byte short
	  */
	def writeShortString(buffer: Buffer, str: String, encoding: Charset = StandardCharsets.UTF_8): Unit = {
		if (str == null) {
			buffer.appendShort(-1)
		} else if (str.length > Short.MaxValue) {
			throw new IllegalArgumentException("String exceeds the maximum size of " + Short.MaxValue + ".")
		} else {
			buffer.appendShort(str.length.asInstanceOf[Short])
			buffer.appendBytes(str.getBytes(encoding))
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
