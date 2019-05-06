package hexagon.controller

import hexagon.exceptions.HexagonException
import hexagon.tools.{Logging, SysTime}
import hexagon.utils.JSON

object ControllerInfo extends Logging {

  def parse(json: String): ControllerInfo = {
    try {
      JSON.fromJson(json, classOf[ControllerInfo]) match {
        case Some(c) => c
        case None => throw new HexagonException(s"Failed to parse the controller info json [$json]")
      }
    } catch {
      case t: Throwable => throw new HexagonException(s"Failed to parse the controller info json [$json]")
    }
  }

}

case class ControllerInfo(brokerId: Int, version: Int = 1, timestamp: Long = SysTime.mills)
