package hexagon.network

import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

import hexagon.exceptions.InvalidRequestException
import hexagon.tools.BYTES

private[hexagon] class BoundedByteBufferReceive(maxSize: Int) extends Receive {

  private val sizeBuffer: ByteBuffer = ByteBuffer.allocate(BYTES.Int)
  private var contentBuffer: ByteBuffer = _

  def this() = this(Int.MaxValue)

  /**
    * 获取内容buffer
    */
  override def buffer: ByteBuffer = {
    expectComplete()
    contentBuffer
  }

  /**
    * 从channel中读取数据
    */
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
