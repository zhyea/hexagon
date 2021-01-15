
package hexagon.exceptions

class InvalidConfigException(message: String) extends RuntimeException(message) {
	def this() = this(null)
}
