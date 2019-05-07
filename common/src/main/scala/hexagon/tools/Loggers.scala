package hexagon.tools

object Loggers {

  val stateChangeLogger: Logging = StateChangeLogger("state.change")

  case class StateChangeLogger(name: String) extends Logging


  val requestLogger: Logging = RequestLogger("request")

  case class RequestLogger(name: String) extends Logging

}
