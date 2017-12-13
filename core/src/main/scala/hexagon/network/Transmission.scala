package hexagon.network


import java.nio._
import java.nio.channels._

import hexagon.utils.Logging


/**
  * 表示hexagon和网络间有状态的数据传输
  */
private[network] trait Transmission extends Logging {

  def complete: Boolean

  protected def expectIncomplete(): Unit = {
    if (complete)
      throw new IllegalStateException("This operation cannot be completed on a complete request.")
  }

  protected def expectComplete(): Unit = {
    if (!complete)
      throw new IllegalStateException("This operation cannot be completed on an incomplete request.")
  }

}


/**
  * 表示从channel接收数据时的交互
  */
private[hexagon] trait Receive extends Transmission {

  def buffer: ByteBuffer

  def readFrom(channel: ReadableByteChannel): Int

  def readCompletely(channel: ReadableByteChannel): Int = {
    var read = 0
    while (!complete) {
      read = readFrom(channel)
      trace(read + " bytes read.")
    }
    read
  }

}


/**
  * 表示通过channel发送数据时的交互
  */
private[hexagon] trait Send extends Transmission {

  def writeTo(channel: GatheringByteChannel): Int

  def writeCompletely(channel: GatheringByteChannel): Int = {
    var written = 0
    while (!complete) {
      written = writeTo(channel)
      trace(written + " bytes written.")
    }
    written
  }

}


/**
  * 处理多个Send实例的集合，会一个接一个的进行处理
  */
abstract class MultiSend[S <: Send](val sends: List[S]) extends Send {

  val expectedBytesToWrite: Int
  private var current = sends
  var totalWritten = 0

  def writeTo(channel: GatheringByteChannel): Int = {
    expectIncomplete
    val written = current.head.writeTo(channel)
    totalWritten += written
    if (current.head.complete)
      current = current.tail
    written
  }

  def complete: Boolean = {
    if (current == Nil) {
      if (totalWritten != expectedBytesToWrite)
        error("mismatch in sending bytes over socket; expected: " + expectedBytesToWrite + " actual: " + totalWritten)
      return true
    }
    else
      return false
  }
}
