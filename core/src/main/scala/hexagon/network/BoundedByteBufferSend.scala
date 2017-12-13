package hexagon.network

import java.nio.ByteBuffer
import java.nio.channels.GatheringByteChannel

import hexagon.utils.nonThreadSafe


@nonThreadSafe
private[hexagon] class BoundedByteBufferSend(val buffer: ByteBuffer) extends Send {

  private val sizeBuffer = ByteBuffer.allocate(4)

  if (buffer.remaining > Int.MaxValue - sizeBuffer.limit)
    throw new IllegalArgumentException(
      ("Attempt to create a bounded buffer of %d bytes, but the maximum allowable size for a bounded buffer is %d.")
        .format(buffer.remaining, (Int.MaxValue - sizeBuffer.limit))
    )

  sizeBuffer.putInt(buffer.limit)
  sizeBuffer.rewind()

  def this(size: Int) = this(ByteBuffer.allocate(size))

  def this(request: Request) = {
    this(request.sizeInBytes + 2)
    buffer.putShort(request.id)
    request.writeTo(buffer)
    buffer.rewind()
  }

  override var complete: Boolean = false

  override def writeTo(channel: GatheringByteChannel): Int = {
    expectIncomplete()
    val written = channel.write(Array(sizeBuffer, buffer))
    if (!buffer.hasRemaining)
      complete = true
    written.toInt
  }

}
