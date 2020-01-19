package hexagon.protocol

class MessageSizeTooLargeException(message: String) extends RuntimeException(message) {

	def this() = this(null)

	def this(payloadSize: Int, maxMessageSize: Int) = this(s"payload size of $payloadSize larger than $maxMessageSize")

}
