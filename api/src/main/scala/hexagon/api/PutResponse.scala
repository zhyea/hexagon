package hexagon.api

import java.nio.ByteBuffer

import hexagon.exceptions.InvalidResponseException
import hexagon.tools.BYTES
import hexagon.utils.IOUtils._


object PutResponse {

  def readFrom(buffer: ByteBuffer): PutResponse = {
    val requestId: Short = buffer.getShort
    if (requestId != RequestKeys.Put) {
      throw new InvalidResponseException(s"Response id is invalid, expected id:${RequestKeys.Put}, actual:$requestId")
    }
    val topic = readShortString(buffer)
    val result = buffer.get == 1

    PutResponse(topic, result)
  }

}


case class PutResponse(topic: String, result: Boolean) extends RequestOrResponse(RequestKeys.Put) {

  override def sizeInBytes: Int = shortStringLength(topic) + BYTES.Byte

  override def writeTo(buffer: ByteBuffer): Unit = {
    writeShortString(buffer, topic)
    buffer.put((if (result) 1 else 0).toByte)
  }

  override def toString: String = s"PutResponse(topic:$topic,result:$result)"
}
