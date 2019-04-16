package hexagon.network

import java.nio.ByteBuffer
import java.nio.channels.GatheringByteChannel

import hexagon.api.RequestOrResponse
import hexagon.tools.BYTES


private[hexagon] class BoundedByteBufferSend(val buffer: ByteBuffer) extends Send {

  private val sizeBuffer = ByteBuffer.allocate(BYTES.Int)

  if (buffer.remaining() > Int.MaxValue - sizeBuffer.limit)
    throw new IllegalArgumentException(s"Attempt to create a bounded buffer of ${buffer.remaining} bytes, "
      + s"but the maximum allowable size for a bounded buffer is ${Int.MaxValue - sizeBuffer.limit}")

  sizeBuffer.putInt(buffer.limit())
  sizeBuffer.rewind()

  def this(size: Int) = this(ByteBuffer.allocate(size))

  def this(response: RequestOrResponse) = {
    this(BYTES.Short + response.sizeInBytes) // requestId + buffer
    buffer.putShort(response.id)
    response.writeTo(buffer)
    buffer.rewind()
  }


  /**
    * 向channel中写数据
    */
  override def writeTo(channel: GatheringByteChannel): Int = {
    expectIncomplete()
    val written = channel.write(Array(sizeBuffer, buffer))
    if (!buffer.hasRemaining) {
      complete.set(true)
    }
    written.toInt
  }

}
