package hexagon.api

import java.nio.ByteBuffer

object MultiPutRequest {

  def readFrom(buffer: ByteBuffer): MultiQueryRequest = ???

}


case class MultiPutRequest() extends RequestOrResponse(RequestKeys.MultiPut) {

  override def sizeInBytes: Int = ???

  override def writeTo(buffer: ByteBuffer): Unit = ???
}