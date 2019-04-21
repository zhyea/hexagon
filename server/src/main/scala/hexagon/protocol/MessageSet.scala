package hexagon.protocol

import java.nio.ByteBuffer
import java.nio.channels.GatheringByteChannel

import hexagon.exceptions.InvalidEntityException
import hexagon.tools.Logging


object MessageSet {

  def messageSetSize(entities: Iterable[Message]): Int = entities.foldLeft(0)(_ + _.serializedSize)

  def createByteBuffer(compressionCodec: CompressionCodec, msgs: Message*): ByteBuffer =
    compressionCodec match {
      case NoCompressionCodec =>
        val buffer = ByteBuffer.allocate(messageSetSize(msgs))
        for (entity <- msgs) {
          entity.serializeTo(buffer)
        }
        buffer.rewind
        buffer

      case _ =>
        msgs.size match {
          case 0 =>
            val buffer = ByteBuffer.allocate(messageSetSize(msgs))
            buffer.rewind
            buffer
          case _ =>
            val entity = CompressionUtils.compress(msgs, compressionCodec)
            val buffer = ByteBuffer.allocate(entity.serializedSize)
            entity.serializeTo(buffer)
            buffer.rewind
            buffer
        }
    }

}


abstract class MessageSet extends Iterable[MessageAndOffset] with Logging {


  def writeTo(channel: GatheringByteChannel, offset: Long, maxSize: Long): Long


  def iterator: Iterator[MessageAndOffset]


  def sizeInBytes: Long


  def validate(): Unit = {
    for (msgAndOffset <- this)
      if (!msgAndOffset.msg.isValid)
        throw new InvalidEntityException()
  }

}
