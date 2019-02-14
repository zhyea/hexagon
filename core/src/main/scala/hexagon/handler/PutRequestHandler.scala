package hexagon.handler

import hexagon.api.PutRequest
import hexagon.network.{Receive, Send}
import hexagon.tools.SysTime

class PutRequestHandler extends Handler {


  override def name: String = "PutRequest"

  override def handle(receive: Receive): Option[Send] = {
    val start = SysTime.mills
    val request = PutRequest.readFrom(receive.buffer)

    if (logger.isTraceEnabled())
      logger.trace(s"Put request $request")


    ???
  }


  private def handle(request: PutRequest): Long = {
    ???
  }

}
