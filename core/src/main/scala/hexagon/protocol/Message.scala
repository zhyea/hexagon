package hexagon.protocol

import java.nio.ByteBuffer
import hexagon.utils.NumberUtils._

object Message {

	/**
	  * 魔数
	  */
	val MagicVersion: Byte = 'W'

	/**
	  * 魔数长度
	  */
	val MagicLength: Int = 1
	/**
	  * CRC校验位长度
	  */
	val CrcLength: Int = 1
	/**
	  * 魔数在消息中的offset
	  */
	val MagicOffset: Int = 0
	/**
	  * CRC校验位在消息中的offset
	  */
	val CrcOffset: Int = MagicOffset + MagicLength
	/**
	  * 消息体在消息中的offset
	  */
	val PayloadOffset: Int = MagicLength + CrcLength
	/**
	  * 消息头在消息中的offset
	  */
	val HeaderSize: Int = PayloadOffset

}


/**
  * 请求协议
  *
  * 1. 1 byte 'magic' identifier
  * 2. 4 byte CRC32 of payload
  * 3. N-6 byte payload
  */
class Message(val buffer: ByteBuffer) {

	import Message._

	private def this(checksum: Long, bytes: Array[Byte]) = {
		this(ByteBuffer.allocate(Message.HeaderSize + bytes.length))
		buffer.put(MagicVersion)
		buffer.putInt(toUnsignedInt(checksum))
		buffer.put(bytes)
		buffer.rewind()
	}

	def this(bytes: Array[Byte]) = this(crc32(bytes), bytes)

	private def size: Int = buffer.limit()

	private def magic: Byte = buffer.get()

	def payloadSize(): Int = size - HeaderSize

	def checksum: Long = toUnsignedInt(buffer.getInt(CrcOffset))

	def payload: ByteBuffer = {
		var payload = buffer.duplicate()
		payload.position(PayloadOffset)
		payload = payload.slice()
		payload.limit(payloadSize())
		payload.rewind()
		payload
	}

	/**
	  * 校验消息是否有效，检查CRC32是否一致
	  */
	def isValid: Boolean =
		checksum == crc32(buffer.array(), buffer.position() + buffer.arrayOffset() + PayloadOffset, payloadSize())

	def serializedSize: Int = Integer.BYTES + buffer.limit()

	/**
	  * 序列化到其他ByteBuffer对象
	  */
	def serializeTo(serBuffer: ByteBuffer): ByteBuffer = {
		serBuffer.putInt(buffer.limit())
		serBuffer.put(buffer.duplicate())
	}

	override def hashCode(): Int = buffer.hashCode()

	override def equals(any: Any): Boolean = {
		any match {
			case that: Message => size == that.size && checksum == that.checksum && payload == that.payload && magic == that.magic
			case _ => false
		}
	}

	override def toString: String = s"Message(magic=$magic, crc=$checksum, payload=$payload)"
}
