package hexagon.protocol

import java.nio.ByteBuffer

import hexagon.tools.BYTES
import hexagon.utils.NumUtils._

object Message {

  val MagicCode: Byte = 18

  val MagicLength: Int = BYTES.Byte
  val CodecLength: Int = BYTES.Byte
  val CrcLength: Int = BYTES.Int

  val MagicOffset: Int = 0 //消息体起始offset
  val CodecOffset: Int = MagicOffset + MagicLength //压缩格式起始offset
  val CrcOffset: Int = CodecOffset + CodecLength //CRC校验码起始offset


  /**
    * Specifies the mask for the compression code. 2 bits to hold the compression codec.
    * 0 is reserved to indicate no compression
    */
  val CompressionCodeMask: Int = 0x03

  /**
    * 消息体起始offset
    */
  def payloadOffset(): Int = MagicLength + CodecLength + CrcLength

  /**
    * 消息头(即消息体前的部分)的大小
    */
  def headerSize(): Int = payloadOffset()

}


/**
  * 每条请求的数据结构的封装
  *
  * 1. 1 byte 魔数标志位 （magic identifier）
  * 2. 1 byte 压缩类型ID
  * 3. 4 byte 消息体CRC32校验码
  * 4. N-6 byte 消息体
  */
private[hexagon] class Message(val buffer: ByteBuffer) {

  import Message._

  def this(checksum: Long, bytes: Array[Byte], compressionCodec: CompressionCodec) = {

    this(ByteBuffer.allocate(Message.headerSize() + bytes.length)) //分配空间

    buffer.put(MagicCode) //写入魔数
    var codec: Byte = NoCompressionCodec.codec.toByte
    if (compressionCodec.codec > 0) {
      codec = (codec | (CompressionCodeMask & compressionCodec.codec)).toByte
    }
    buffer.put(codec) //写入压缩类型ID
    buffer.putInt(toUnsignedInt(checksum)) //写入CRC32校验码
    buffer.put(bytes) //写入消息体数据
    buffer.rewind() //重置，便于读取
  }

  def this(bytes: Array[Byte], compressionCodec: CompressionCodec = NoCompressionCodec) = this(crc32(bytes), bytes, compressionCodec)

  def size: Int = buffer.limit()

  def payloadSize(): Int = size - headerSize()

  def magic: Byte = buffer.get(0)

  def codec: Byte = buffer.get(CodecOffset)

  def compressionCodec: CompressionCodec = CompressionCodec.getCompressionCodec(codec & CompressionCodeMask)

  /**
    * CRC校验码
    */
  def checksum: Long = toUnsignedInt(buffer.getInt(CrcOffset))

  /**
    * 消息体Buffer
    */
  def payload: ByteBuffer = {
    var payload = buffer.duplicate()
    payload.position(payloadOffset())
    payload = payload.slice()
    payload.limit(payloadSize())
    payload.rewind()
    payload
  }

  /**
    * 消息校验
    */
  def isValid: Boolean =
    checksum == crc32(buffer.array(), buffer.position() + buffer.arrayOffset() + payloadOffset(), payloadSize())

  /**
    * 将消息序列化到Buffer中，结构：消息总长度（魔数长度 + 压缩类型长度 + CRC校验码长度 + 消息体长度） + 数据体
    */
  def serializeTo(serBuffer: ByteBuffer): ByteBuffer = {
    serBuffer.putInt(buffer.limit())
    serBuffer.put(buffer.duplicate())
  }

  /**
    * Entity序列化后占用的空间：序列化后的长度 + Entity占用空间
    */
  def serializedSize: Int = BYTES.Int + buffer.limit()

  override def hashCode(): Int = buffer.hashCode()

  override def equals(any: Any): Boolean = {
    any match {
      case that: Message => size == that.size && codec == that.codec && checksum == that.checksum && payload == that.payload && magic == that.magic
      case _ => false
    }
  }

  override def toString: String = s"Entity(magic=$magic, codec=$codec, crc=$checksum, payload=$payload)"
}
