package hexagon.utils

import org.slf4j.{Logger, LoggerFactory}

trait Logging {

  lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)


  protected var logIdent = ""


  private def formatWithLogIdent(msg: String) = "%s%s".format(logIdent, msg)

  def trace(msg: => String, args: Any*): Unit = {
    if (logger.isTraceEnabled)
      logger.trace(formatWithLogIdent(msg), args)
  }

  def debug(msg: => String, args: Any*): Unit = {
    if (logger.isDebugEnabled)
      logger.debug(formatWithLogIdent(msg), args)
  }

  def info(msg: => String, args: Any*): Unit = {
    if (logger.isInfoEnabled)
      logger.info(formatWithLogIdent(msg), args)
  }

  def warn(msg: => String, args: Any*): Unit = {
    if (logger.isWarnEnabled)
      logger.warn(formatWithLogIdent(msg), args)
  }

  def error(msg: => String, args: Any*): Unit = {
    if (logger.isErrorEnabled)
      logger.error(formatWithLogIdent(msg), args)
  }

}
