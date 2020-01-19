package hexagon.exceptions

class HexagonConnectException(message: String, t: Throwable) extends RuntimeException(message, t) {

	def this(message: String) = this(message, null)

	def this(t: Throwable) = this("", t)

}
