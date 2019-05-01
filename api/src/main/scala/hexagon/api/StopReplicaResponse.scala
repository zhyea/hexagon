package hexagon.api

import java.nio.ByteBuffer

import hexagon.exceptions.ErrorMapping
import hexagon.tools.BYTES
import hexagon.utils.IOUtils.{readShortString, writeShortString}

import scala.collection.Map
import scala.collection.mutable.HashMap

object StopReplicaResponse {

  def readFrom(buffer: ByteBuffer): StopReplicaResponse = {
    val correlationId = buffer.getInt
    val errorCode = buffer.getShort()
    val responseSize = buffer.getInt()
    val response = new HashMap[String, Short]()
    for (i <- 0 until responseSize) {
      val topic = readShortString(buffer)
      val topicErrorCode = buffer.getShort
      response.put(topic, topicErrorCode)
    }
    StopReplicaResponse(correlationId, response, errorCode)
  }

}


case class StopReplicaResponse(correlationId: Int, //交互ID
                               response: Map[String, Short], //topic->topicErrorCode
                               errorCode: Short = ErrorMapping.NoError // response Error Code
                              ) extends RequestOrResponse(RequestKeys.StopReplica) {


  override def sizeInBytes: Int = {
    var size =
      BYTES.Int + // correlationId
        BYTES.Short + // ErrorCode
        BYTES.Int // response size

    for ((k, v) <- response) {
      size +=
        BYTES.Short + k.length + //topic
          BYTES.Short // topic ErrorCode
    }

    size
  }

  override def writeTo(buffer: ByteBuffer): Unit = {
    buffer.putInt(correlationId)
    buffer.putShort(errorCode)
    buffer.putInt(response.size)
    for ((k, v) <- response) {
      writeShortString(buffer, k)
      buffer.putShort(v)
    }
  }

}
