package hexagon.handler

import hexagon.network.{Receive, Send}
import hexagon.tools.Logging
import org.slf4j.LoggerFactory

trait Handler extends Logging {


	protected val logger = LoggerFactory.getLogger("hexagon.request.logger")


	def name: String

	def handle(receive: Receive): Option[Send]

}
