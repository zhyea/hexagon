package hexagon.network

import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

import hexagon.exceptions.HexagonException
import hexagon.tools.Logging

private[hexagon] trait Transmission extends Logging {

  def complete: Boolean


  def expectComplete() = {
    if (!complete) throw new HexagonException("This operation cannot be completed on an incomplete request.")
  }


  def expectIncomplete() = {
    if (complete) throw new HexagonException("This operation cannot be completed on a complete request.")
  }

}


trait Receive extends Transmission {

  def buffer: ByteBuffer

  def readFrom(channel: ReadableByteChannel)

  def readComplete(channel: ReadableByteChannel): Int = {
    var totalRead = 0
    while (!complete) {
      totalRead += channel.read(buffer)
    }
    totalRead
  }

}


trait Send extends Transmission