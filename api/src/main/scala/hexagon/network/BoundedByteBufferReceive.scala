package hexagon.network

import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

import hexagon.exceptions.InvalidRequestException

private[hexagon] class BoundedByteBufferReceive(maxSize: Int) extends Receive {

  private val sizeBuffer: ByteBuffer = ByteBuffer.allocate(Integer.BYTES)
  private var contentBuffer: ByteBuffer = _

  def this() = this(Int.MaxValue)

  override def buffer: ByteBuffer = {
    expectComplete()
    contentBuffer
  }

  override def readFrom(channel: ReadableByteChannel): Int = {
    expectIncomplete()

    var read = 0
    if (sizeBuffer.remaining > 0)
      read += channel.read(sizeBuffer)

    if (null == contentBuffer && !sizeBuffer.hasRemaining) {
      sizeBuffer.rewind()
      val size = sizeBuffer.getInt()
      if (size < 0) throw new InvalidRequestException(s"$size is not a valid size.")
      if (size > maxSize) throw new InvalidRequestException(s"$size is larger than max size of $maxSize")
      contentBuffer = ByteBuffer.allocate(size)
    }

    if (null != contentBuffer) {
     read += channel.read(contentBuffer)
      if (!contentBuffer.hasRemaining) {
        contentBuffer.rewind()
        complete.set(true)
      }
    }
    read
  }

}
