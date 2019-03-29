package hexagon.api

import java.nio.ByteBuffer

import hexagon.tools.Logging

abstract class RequestOrResponse(val id: Short) extends Logging {

  def sizeInBytes: Int

  def writeTo(buffer: ByteBuffer): Unit

}
