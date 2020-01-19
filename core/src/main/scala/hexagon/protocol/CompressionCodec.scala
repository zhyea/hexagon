package hexagon.protocol

import hexagon.exceptions.UnknownCodecException


object CompressionCodec {

	def getCompressionCodec(codec: Int): CompressionCodec = {
		codec match {
			case NoCompressionCodec.codec => NoCompressionCodec
			case GZIPCompressionCodec.codec => GZIPCompressionCodec
			case _ => throw new UnknownCodecException(s"$codec is an unknown compression codec")
		}
	}

}


sealed trait CompressionCodec {
	def codec: Int
}

case object NoCompressionCodec extends CompressionCodec {
	val codec: Int = 0
}

case object GZIPCompressionCodec extends CompressionCodec {
	val codec: Int = 1
}

case object DefaultCompressionCodec extends CompressionCodec {
	val codec: Int = GZIPCompressionCodec.codec
}