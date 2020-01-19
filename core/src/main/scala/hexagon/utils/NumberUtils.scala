package hexagon.utils

import java.util.zip.CRC32

object NumberUtils {


	/**
	  * Change the given int value as a 4 byte unsigned integer.
	  */
	def toUnsignedInt(value: Int): Long = {
		value & 0xfffffffL
	}


	/**
	  * Change the given long value as a 4 byte unsigned integer. Overflow is ignored.
	  */
	def toUnsignedInt(value: Long): Int = {
		(value & 0xfffffffL).asInstanceOf[Int]
	}


	/**
	  * Compute the CRC32 of the byte array
	  */
	def crc32(bytes: Array[Byte]): Long = {
		crc32(bytes, 0, bytes.length)
	}


	/**
	  * 执行crc32运算
	  *
	  * @param bytes  目标数组
	  * @param offset 起始offset
	  * @param size   要计算的长度
	  * @return 计算结果
	  */
	def crc32(bytes: Array[Byte], offset: Int, size: Int): Long = {
		val crc = new CRC32()
		crc.update(bytes, offset, size)
		crc.getValue
	}


	def hashcode(as: Any*): Int = {
		if (as == null)
			return 0
		var h = 1
		var i = 0
		while (i < as.length) {
			if (as(i) != null) {
				h = 31 * h + as(i).hashCode
				i += 1
			}
		}
		h
	}
}
