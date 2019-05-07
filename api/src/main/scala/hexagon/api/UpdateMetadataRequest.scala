package hexagon.api

import java.nio.ByteBuffer


object UpdateMetadataRequest {

  def readFrom(byteBuffer: ByteBuffer): UpdateMetadataRequest = ???
  
}


case class UpdateMetadataRequest() extends RequestOrResponse(RequestKeys.UpdateMetadata) {

  override def sizeInBytes: Int = ???

  override def writeTo(buffer: ByteBuffer): Unit = ???

}