package hexagon.network

import java.nio.ByteBuffer
import java.nio.channels.GatheringByteChannel

private[hexagon] class ByteBufferSend(val buffer: ByteBuffer) extends Send {


  def this(size: Int) = this(ByteBuffer.allocate(size))

  override var complete: Boolean = false

  override def writeTo(channel: GatheringByteChannel): Int = {
    expectIncomplete()
    val written = channel.write(buffer)
    if (!buffer.hasRemaining) {
      complete = true
    }
    written
  }


}
