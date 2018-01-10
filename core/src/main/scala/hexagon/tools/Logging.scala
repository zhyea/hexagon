package hexagon.tools

import org.slf4j.{Logger, LoggerFactory}

class Logging {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def trace(format: String, args: Any*) = {
    logger.trace(format, args)
  }

  def info(format: String, args: Any*) = {
    logger.trace(format, args)
  }

  def warn(format: String, args: Any*) = {
    logger.trace(format, args)
  }

  def error(format: String, args: Any*) = {
    logger.trace(format, args)
  }

}
