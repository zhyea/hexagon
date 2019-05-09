package hexagon.api

import java.nio.ByteBuffer
import java.util.Objects

import hexagon.tools.BYTES
import hexagon.utils.IOUtils._


object PutRequest {

  def readFrom(buffer: ByteBuffer): PutRequest = {
    val correlationId = buffer.getInt
    val topic = readShortString(buffer)
    val msg = readShortString(buffer)
    PutRequest(correlationId, topic, msg)
  }

}


case class PutRequest(correlationId: Int, // 交互ID
                      topic: String, // topic
                      msg: String // 消息内容
                     ) extends RequestOrResponse(RequestKeys.Put) {

  /**
    * topicLength + topic + msgLength + msg
    */
  override def sizeInBytes: Int =
    BYTES.Int +
      BYTES.Short + topic.length +
      BYTES.Short +
      msg.length

  override def writeTo(buffer: ByteBuffer): Unit = {
    writeShortString(buffer, topic)
    writeShortString(buffer, msg)
  }

  override def toString: String = s"PutRequest(topic:$topic,msg:$msg)"

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
