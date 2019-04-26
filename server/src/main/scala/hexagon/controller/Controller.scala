package hexagon.controller

import hexagon.exceptions.HexagonException
import hexagon.tools.{Logging, SysTime}
import hexagon.utils.JSON

object Controller extends Logging {

  def parse(json: String): Controller = {
    try {
      JSON.fromJson(json, classOf[Controller]) match {
        case Some(c) => c
        case None => throw new HexagonException(s"Failed to parse the controller info json [$json]")
      }
    } catch {
      case t: Throwable => throw new HexagonException(s"Failed to parse the controller info json [$json]")
    }
  }

}

case class Controller(brokerId: Int, version: Int = 1, timestamp: Long = SysTime.mills)
