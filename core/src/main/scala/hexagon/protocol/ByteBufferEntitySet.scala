package hexagon.protocol

import java.nio.ByteBuffer
import java.nio.channels.GatheringByteChannel

import hexagon.exceptions.{InvalidEntityException, InvalidEntitySizeException}
import hexagon.tools.ItrTemplate

class ByteBufferEntitySet(private val buffer: ByteBuffer,
                          private val initOffset: Long = 0L) extends EntitySet {

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

  override def sizeInBytes: Int = buffer.limit()


  override def toString: String = {
    val builder = new StringBuilder()
    builder.append("ByteBufferEntitySet(")
    for (entity <- this) {
      builder.append(entity)
      builder.append(", ")
    }
    builder.append(")")
    builder.toString
  }

  override def equals(other: Any): Boolean = {
    other match {
      case that: ByteBufferEntitySet =>
        (that canEqual this) && buffer.equals(that.buffer) && initOffset == that.initOffset
      case _ => false
    }
  }

  override def canEqual(other: Any): Boolean = other.isInstanceOf[ByteBufferEntitySet]

  override def hashCode: Int = 31 + buffer.hashCode + initOffset.hashCode

  override def iterator: Iterator[EntityAndOffset] = new Itr()

  def shallowIterator: Iterator[EntityAndOffset] = new Itr(true)

  def verifyEntitySize(maxEntitySize: Int): Unit = {
    for (e <- shallowIterator) {
      val payloadSize = e.entity.payloadSize()
      if (payloadSize > maxEntitySize)
        throw new EntitySizeTooLargeException(payloadSize, maxEntitySize)
    }
  }

  private def internalIterator(): Iterator[EntityAndOffset] = new Itr()

  private class Itr(isShallow: Boolean = false) extends ItrTemplate[EntityAndOffset] {

    var outerBuffer: ByteBuffer = buffer.slice()
    var currValidBytes: Long = initOffset
    var innerItr: Iterator[EntityAndOffset] = _
    var lastEntitySize: Long = 0L

    def makeNextOuter(): EntityAndOffset = {
      if (outerBuffer.remaining() < Integer.BYTES) {
        return done()
      }

      val size = outerBuffer.getInt
      lastEntitySize = size

      trace(s"Remaining bytes in iterator = ${outerBuffer.remaining()}")
      trace(s"Size of data = $size")

      if (size < 0 || size > outerBuffer.remaining()) {
        if (size < 0 || currValidBytes == initOffset) {
          throw new InvalidEntitySizeException(
            s"""
               |Invalid entity size: $size, only received bytes: ${outerBuffer.remaining()} at $currValidBytes.
               |Possible causes: (1) a single message larger than the fetch size; (2) log corruption.
             """.stripMargin
          )
        }
        return done()
      }

      val buffer = outerBuffer.slice()
      buffer.limit(size)
      outerBuffer.position(outerBuffer.position() + size)

      val newEntity = new Entity(buffer)
      if (!newEntity.isValid) {
        throw new InvalidEntityException(s"Entity is invalid, compression codec: ${newEntity.compressionCodec}, size: $size, curr offset: $currValidBytes, init offset:$initOffset. ")
      }

      if (isShallow) {
        currValidBytes += 4 + size
        trace(s"Shallow iterator currValidBytes = $currValidBytes")
        EntityAndOffset(newEntity, currValidBytes)
      } else {
        newEntity.compressionCodec match {
          case NoCompressionCodec =>
            innerItr = _
            debug(s"Entity is uncompressed. Valid byte count = $currValidBytes")
            currValidBytes += 4 + size
            trace(s"currValidBytes = $currValidBytes")
            EntityAndOffset(newEntity, currValidBytes)
          case _ =>
            debug(s"Entity is compressed. Valid byte count = $currValidBytes")
            innerItr = CompressionUtils.decompress(newEntity).internalIterator()
            if (!innerItr.hasNext) {
              currValidBytes += 4 + lastEntitySize
              innerItr = _
            }
            makeNext()
        }

      }
    }

    override protected def makeNext(): EntityAndOffset = {
      if (isShallow) {
        makeNextOuter()
      } else {
        val isInnerDone = null == innerItr || !innerItr.hasNext
        if (isInnerDone) {
          makeNextOuter()
        } else {
          val eao = innerItr.next()
          if (!innerItr.hasNext)
            currValidBytes += 4 + lastEntitySize
          EntityAndOffset(eao.entity, currValidBytes)
        }
      }
    }
  }

}
