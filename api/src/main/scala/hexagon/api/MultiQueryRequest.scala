package hexagon.api

import java.nio.ByteBuffer

object MultiQueryRequest {

  def readFrom(buffer: ByteBuffer): MultiQueryRequest = ???

}


case class MultiQueryRequest() extends RequestOrResponse(RequestKeys.MultiQuery) {

  override def sizeInBytes: Int = ???

  override def writeTo(buffer: ByteBuffer): Unit = ???
}