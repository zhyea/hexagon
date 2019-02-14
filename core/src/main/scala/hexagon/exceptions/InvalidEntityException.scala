package hexagon.exceptions

class InvalidEntityException(message: String) extends RuntimeException(message) {
  def this() = this(null)
}
