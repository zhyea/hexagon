
package hexagon.exception

class InvalidRequestException(val message: String) extends RuntimeException(message) {

  def this() = this("")

}
