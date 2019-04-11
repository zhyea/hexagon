package hexagon.api

import java.nio.ByteBuffer
import java.util.Objects

import hexagon.network.RequestOrResponse
import hexagon.tools.Bytes
import hexagon.utils.IOUtils._


object PutRequest {

  def readFrom(buffer: ByteBuffer): PutRequest = {
    val topic = readShortString(buffer)
    val msg = readShortString(buffer)
    new PutRequest(topic, msg)
  }

}


class PutRequest(val topic: String,
                 val msg: String) extends RequestOrResponse(RequestKeys.Put) {

  /**
    * topicLength + topic + msgLength + msg
    */
  override def sizeInBytes: Int = Bytes.Short + topic.length + Bytes.Short + msg.length

  override def writeTo(buffer: ByteBuffer): Unit = {
    writeShortString(buffer, topic)
    writeShortString(buffer, msg)
  }

  override def toString: String = {
    val builder = new StringBuilder()
    builder.append("PutRequest(")
    builder.append(topic + ",")
    builder.append(msg)
    builder.append(")")
    builder.toString
  }

  override def equals(other: Any): Boolean = {
    other match {
      case that: PutRequest =>
        (that canEqual this) && topic == that.topic && msg == that.msg
      case _ => false
    }
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[PutRequest]

  override def hashCode(): Int = Objects.hash(topic, msg)
}
