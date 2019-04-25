package hexagon.controller

import hexagon.tools.SysTime

case class ControllerInfo(brokerId: Int, version: Int = 1, timestamp: Long = SysTime.mills)
