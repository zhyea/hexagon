package hexagon.protocol

import java.nio.ByteBuffer

import hexagon.tools.BYTES
import hexagon.utils.NumUtils._

object Entity {

  val MagicVersion: Byte = 0

  val MagicLength: Int = 1
  val CodecLength: Int = 1
  val CrcLength: Int = 1

  val MagicOffset: Int = 0
  val CodecOffset: Int = MagicOffset + MagicLength
  val CrcOffset: Int = CodecOffset + CodecLength


  /**
    * Specifies the mask for the compression code. 2 bits to hold the compression codec.
    * 0 is reserved to indicate no compression
    */
  val CompressionCodeMask: Int = 0x03

  def payloadOffset(): Int = MagicLength + CodecLength + CrcLength

  def headerSize(): Int = payloadOffset()

}


/**
  * Request entity. Detail is the following:
  *
  * 1. 1 byte 'magic' identifier
  * 2. 1 byte type of codec used
  * 3. 4 byte CRC32 of payload
  * 4. N-6 byte payload
  */
private[hexagon] class Entity(val buffer: ByteBuffer) {

  import Entity._

  def this(checksum: Long, bytes: Array[Byte], compressionCodec: CompressionCodec) = {
    this(ByteBuffer.allocate(Entity.headerSize() + bytes.length))
    buffer.put(MagicVersion)
    var codec: Byte = NoCompressionCodec.codec.toByte
    if (compressionCodec.codec > 0) {
      codec = (codec | (CompressionCodeMask & compressionCodec.codec)).toByte
    }
    buffer.put(codec)
    buffer.putInt(toUnsignedInt(checksum))
    buffer.put(bytes)
    buffer.rewind()
  }

  def this(bytes: Array[Byte], compressionCodec: CompressionCodec = NoCompressionCodec) = this(crc32(bytes), bytes, compressionCodec)

  def size: Int = buffer.limit()

  def payloadSize(): Int = size - headerSize()

  def magic: Byte = buffer.get()

  def codec: Byte = buffer.get(CodecOffset)

  def compressionCodec: CompressionCodec = CompressionCodec.getCompressionCodec(codec & CompressionCodeMask)

  def checksum: Long = toUnsignedInt(buffer.getInt(CrcOffset))

  def payload: ByteBuffer = {
    var payload = buffer.duplicate()
    payload.position(payloadOffset())
    payload = payload.slice()
    payload.limit(payloadSize())
    payload.rewind()
    payload
  }

  def isValid: Boolean =
    checksum == crc32(buffer.array(), buffer.position() + buffer.arrayOffset() + payloadOffset(), payloadSize())

  def serializedSize: Int = BYTES.Int + buffer.limit()

  def serializeTo(serBuffer: ByteBuffer): ByteBuffer = {
    serBuffer.putInt(buffer.limit())
    serBuffer.put(buffer.duplicate())
  }

  override def hashCode(): Int = buffer.hashCode()

  override def equals(any: Any): Boolean = {
    any match {
      case that: Entity => size == that.size && codec == that.codec && checksum == that.checksum && payload == that.payload && magic == that.magic
      case _ => false
    }
  }

  override def toString: String = s"Entity(magic=$magic, codec=$codec, crc=$checksum, payload=$payload)"
}
