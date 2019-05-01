package hexagon.api

import java.nio.ByteBuffer

import hexagon.exceptions.ErrorMapping

object UpdateMetadataResponse {

  def readFrom(buffer: ByteBuffer): UpdateMetadataResponse = {
    val correlationId = buffer.getInt
    val errorCode = buffer.getShort
    new UpdateMetadataResponse(correlationId, errorCode)
  }

}

case class UpdateMetadataResponse(correlationId: Int,
                                  errorCode: Short = ErrorMapping.NoError)

  extends RequestOrResponse(RequestKeys.UpdateMetadata) {

  override def sizeInBytes: Int = 4 /* correlation id */ + 2 /* error code */

  override def writeTo(buffer: ByteBuffer): Unit = {
    buffer.putInt(correlationId)
    buffer.putShort(errorCode)
  }

}