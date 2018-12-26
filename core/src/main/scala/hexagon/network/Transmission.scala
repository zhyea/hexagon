package hexagon.network

import java.nio.ByteBuffer
import java.nio.channels.{GatheringByteChannel, ReadableByteChannel}

import hexagon.exceptions.HexagonException
import hexagon.tools.Logging

private[hexagon] trait Transmission extends Logging {

  def complete: Boolean


  def expectComplete(): Unit =
    if (!complete) throw new HexagonException("This operation cannot be completed on an incomplete request.")


  def expectIncomplete(): Unit =
    if (complete) throw new HexagonException("This operation cannot be completed on a complete request.")


}


trait Receive extends Transmission {

  def buffer: ByteBuffer

  def readFrom(channel: ReadableByteChannel): Int

  def readComplete(channel: ReadableByteChannel): Int = {
    var totalRead = 0
    while (!complete) {
      totalRead += readFrom(channel)
    }
    totalRead
  }

}


trait Send extends Transmission {

  def writeTo(channel: GatheringByteChannel): Int

  def writeComplete(channel: GatheringByteChannel): Int = {
    var totalWrite = 0
    while (!complete) {
      totalWrite += writeTo(channel)
    }
    totalWrite
  }

}