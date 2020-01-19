package hexagon.tools

import org.slf4j.{Logger, LoggerFactory}

trait Logging {

	private val logger: Logger = LoggerFactory.getLogger(this.getClass)


	def debug(format: String, args: Any*): Unit = {
		if (logger.isDebugEnabled)
			logger.debug(format, args)
	}

	def trace(format: String, args: Any*): Unit = {
		if (logger.isTraceEnabled)
			logger.trace(format, args)
	}

	def info(format: String, args: Any*): Unit = {
		if (logger.isInfoEnabled)
			logger.info(format, args)
	}

	def warn(format: String, args: Any*): Unit = {
		if (logger.isWarnEnabled)
			logger.warn(format, args)
	}

	def error(format: String, args: Any*): Unit = {
		if (logger.isErrorEnabled)
			logger.error(format, args)
	}

	def swallowWarn(action: => Unit, message: String = null): Unit = {
		try {
			action
		} catch {
			case e: Exception => warn(if (null == message) e.getMessage else message, e)
		}
	}

	def swallowError(action: => Unit, message: String = null): Unit = {
		try {
			action
		} catch {
			case e: Exception => error(if (null == message) e.getMessage else message, e)
		}
	}

}
