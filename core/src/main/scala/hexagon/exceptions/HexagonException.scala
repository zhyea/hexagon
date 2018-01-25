package hexagon.exceptions

class HexagonException(message: String, t: Throwable) extends RuntimeException {

  def this(message: String) = this(message, null)

  def this(t: Throwable) = this("", t)

}
