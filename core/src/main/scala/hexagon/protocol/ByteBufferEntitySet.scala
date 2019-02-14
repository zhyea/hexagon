package hexagon.protocol

import java.nio.ByteBuffer
import java.nio.channels.GatheringByteChannel

import hexagon.tools.Logging

class ByteBufferEntitySet(private val buffer: ByteBuffer,
                          private val initOffset: Long = 0L) extends EntitySet with Logging {

  def this(compressionCodec: CompressionCodec, entities: Entity*) = {
    this(EntitySet.createByteBuffer(compressionCodec, entities: _*))
  }


  def this(entities: Entity*) = this(NoCompressionCodec, entities: _*)


  def getBuffer: ByteBuffer = buffer


  def getInitOffset: Long = initOffset


  def serialized: ByteBuffer = buffer


  override def writeTo(channel: GatheringByteChannel, offset: Long, maxSize: Long): Long = {
    buffer.mark()
    val written = channel.write(buffer)
    buffer.reset()
    written
  }

  override def iterator: Iterator[Entity] = {

    new Iterator[Entity]() {

      override def hasNext: Boolean = ???

      override def next(): Entity = ???
    }
  }


  private def internalIterator(isShallow: Boolean = false): Iterator[Entity] = {
    new Iterator[Entity]() {

      var topItr = buffer.slice()

      override def hasNext: Boolean = ???

      override def next(): Entity = ???
    }
  }


  override def sizeInBytes: Int = buffer.limit()
}
