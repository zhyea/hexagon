package hexagon.protocol

class EntitySizeTooLargeException(message: String) extends RuntimeException(message) {

	def this() = this(null)

	def this(payloadSize: Int, maxEntitySize: Int) = this(s"payload size of $payloadSize larger than $maxEntitySize")

}
