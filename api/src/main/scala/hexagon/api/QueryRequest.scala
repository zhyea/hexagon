package hexagon.api

import java.nio.ByteBuffer

object QueryRequest {

  def readFrom(buffer: ByteBuffer): QueryRequest = ???

}


case class QueryRequest() extends RequestOrResponse(RequestKeys.Query) {

  override def sizeInBytes: Int = ???

  override def writeTo(buffer: ByteBuffer): Unit = ???
}
