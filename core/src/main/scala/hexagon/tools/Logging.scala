package hexagon.tools

import org.slf4j.{Logger, LoggerFactory}

class Logging {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private[hexagon] def trace(format: String, args: Any*) = {
    logger.trace(format, args)
  }

  private[hexagon] def info(format: String, args: Any*) = {
    logger.trace(format, args)
  }

  private[hexagon] def warn(format: String, args: Any*) = {
    logger.trace(format, args)
  }

  private[hexagon] def error(format: String, args: Any*) = {
    logger.trace(format, args)
  }

}
