package hexagon.api

import java.nio.ByteBuffer

import hexagon.network.RequestOrResponse

case class PutResponse() extends RequestOrResponse(RequestKeys.Put) {


  override def sizeInBytes: Int = ???

  override def writeTo(buffer: ByteBuffer): Unit = throw new UnsupportedOperationException

}
