package hexagon.network

import java.nio.ByteBuffer

private[hexagon] abstract class RequestOrResponse(val requestId: Short) {


	def sizeInBytes: Int

	def writeTo(buffer: ByteBuffer): Unit

}
