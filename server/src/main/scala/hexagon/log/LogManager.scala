package hexagon.log

import java.io.File

import hexagon.config.HexagonConfig
import hexagon.tools.Logging


private[hexagon] object LogManager extends Logging {

  /**
    * 日志目录结构为：日志根目录 + brokerId + topic + 日志文件集合
    */
  private def getOrCreateLogDir(logDir: String): File = {
    val dir: File = new File(logDir)
    if (!dir.exists()) {
      info(s"No log directory found, creating '${dir.getAbsolutePath}'")
      dir.mkdirs()
    }

    if (!dir.isDirectory || !dir.canRead) {
      throw new IllegalStateException(s"${dir.getAbsolutePath} is not a readable log directory.")
    }

    dir
  }

}


private[hexagon] class LogManager(val config: HexagonConfig,
                                  private val time: Long,
                                  needRecovery: Boolean) extends Logging {

  private val logDir = LogManager.getOrCreateLogDir(config.logDir)
  private val lock = new Object


  def getOrCreateLog(): Log = {
    ???
  }


  /**
    * 创建 Log 实例
    */
  private def createLog(topic: String): Unit = {
    lock synchronized {
      val d = new File(logDir, topic)
      d.mkdirs()
      new Log(d, time, config.logFileSize, needRecovery)
    }

  }

}
