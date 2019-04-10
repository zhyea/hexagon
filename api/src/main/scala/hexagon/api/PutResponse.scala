package hexagon.api

import java.nio.ByteBuffer

import hexagon.network.RequestOrResponse
import hexagon.utils.IOUtils._


object PutResponse {

  def readFrom(buffer: ByteBuffer): PutResponse = {
    val topic = readShortString(buffer)
    val result = buffer.get == 1
    PutResponse(topic, result)
  }

}


case class PutResponse(val topic: String,
                       val result: Boolean) extends RequestOrResponse(RequestKeys.Put) {

  override def sizeInBytes: Int = shortStringLength(topic)

  override def writeTo(buffer: ByteBuffer): Unit = {
    writeShortString(buffer, topic)
    buffer.put((if (result) 1 else 0).toByte)
  }

}
