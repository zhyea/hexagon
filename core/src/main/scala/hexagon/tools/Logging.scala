package hexagon.tools

import org.slf4j.{Logger, LoggerFactory}

trait Logging {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)


  def debug(format: String, args: Any*) = {
    logger.debug(format, args)
  }

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

  def swallow(action: => Unit, message: String) = {
    try {
      action
    } catch {
      case e: Exception => error(if (null == message) e.getMessage else message, e)
    }
  }

  def swallow(action: => Unit) = {
    swallow(action, null)
  }

}
