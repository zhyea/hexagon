package hexagon.network

import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

import hexagon.exception.InvalidRequestException
import hexagon.utils.{IOUtils, nonThreadSafe}


@nonThreadSafe
private[hexagon] class BoundedByteBufferReceive(val maxSize: Int) extends Receive {


  private val sizeBuffer: ByteBuffer = ByteBuffer.allocate(4)

  private var contentBuffer: ByteBuffer = null


  def this() = this(Int.MaxValue)

  override var complete: Boolean = false

  override def buffer: ByteBuffer = {
    expectComplete()
    contentBuffer
  }


  override def readFrom(channel: ReadableByteChannel): Int = {
    expectIncomplete()
    var read = 0

    if (sizeBuffer.remaining > 0)
      read += IOUtils.read(channel, sizeBuffer)

    if (null == contentBuffer && sizeBuffer.hasRemaining) {
      sizeBuffer.rewind()
      val size = sizeBuffer.getInt()
      if (size <= 0)
        throw new InvalidRequestException("%d is not a valid request size.".format(size))
      if (size > maxSize)
        throw new InvalidRequestException("Request of length %d is not valid, it is larger than the maximum size of %d bytes.".format(size, maxSize))
      contentBuffer = byteBufferAllocate(size)
    }

    if (null != contentBuffer) {
      read = IOUtils.read(channel, contentBuffer)
      if (contentBuffer.hasRemaining) {
        contentBuffer.rewind()
        complete = true
      }
    }

    read
  }


  private def byteBufferAllocate(size: Int): ByteBuffer = {
    var byteBuffer: ByteBuffer = null
    try {
      byteBuffer = ByteBuffer.allocate(size)
    } catch {
      case e: OutOfMemoryError => {
        error("Out of memory with the size {} ", size, e)
        throw e
      }
      case e2 => throw e2
    }
    byteBuffer
  }

}
