package hexagon.handler

import hexagon.api.BloomRequest
import hexagon.network.{Receive, Send}
import hexagon.utils.SysTime

class PutRequestHandler extends Handler {


	override def name: String = "PutRequest"

	override def handle(receive: Receive): Option[Send] = {
		val start = SysTime.milli
		val request = BloomRequest.readFrom(receive.buffer)

		if (logger.isTraceEnabled())
			logger.trace(s"Put request $request")


		???
	}


	private def handle(request: BloomRequest): Long = {
		???
	}

}
