package hexagon.handler

import hexagon.network.{Receive, Send}
import hexagon.tools.Logging
import org.slf4j.{Logger, LoggerFactory}

trait Handler extends Logging {


  protected val logger: Logger = LoggerFactory.getLogger("hexagon.request.logger")


  def name: String

  def handle(receive: Receive): Option[Send]

}
