package hexagon.protocol

import java.nio.ByteBuffer
import java.nio.channels.GatheringByteChannel

import hexagon.exceptions.InvalidEntityException
import hexagon.tools.Logging


object EntitySet {

  def entitySetSize(entities: Iterable[Entity]): Int = entities.foldLeft(0)(_ + _.serializedSize)

  def createByteBuffer(compressionCodec: CompressionCodec, entities: Entity*): ByteBuffer =
    compressionCodec match {
      case NoCompressionCodec =>
        val buffer = ByteBuffer.allocate(entitySetSize(entities))
        for (entity <- entities) {
          entity.serializeTo(buffer)
        }
        buffer.rewind
        buffer

      case _ =>
        entities.size match {
          case 0 =>
            val buffer = ByteBuffer.allocate(entitySetSize(entities))
            buffer.rewind
            buffer
          case _ =>
            val entity = CompressionUtils.compress(entities, compressionCodec)
            val buffer = ByteBuffer.allocate(entity.serializedSize)
            entity.serializeTo(buffer)
            buffer.rewind
            buffer
        }
    }

}


abstract class EntitySet extends Iterable[EntityAndOffset] with Logging {


  def writeTo(channel: GatheringByteChannel, offset: Long, maxSize: Long): Long


  def iterator: Iterator[EntityAndOffset]


  def sizeInBytes: Long


  def validate(): Unit = {
    for (entityAndOffset <- this)
      if (!entityAndOffset.entity.isValid)
        throw new InvalidEntityException()
  }

}
