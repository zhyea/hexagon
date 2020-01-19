package hexagon.network

import java.nio.ByteBuffer

private[hexagon] abstract class Request(val id: Short) {


	def sizeInBytes: Int

	def writeTo(buffer: ByteBuffer): Unit

}
