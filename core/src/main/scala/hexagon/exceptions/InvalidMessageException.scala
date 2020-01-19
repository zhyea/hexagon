package hexagon.exceptions

class InvalidMessageException(message: String) extends RuntimeException(message) {

	def this() = this(null)

}
