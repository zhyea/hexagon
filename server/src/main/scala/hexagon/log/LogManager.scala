package hexagon.log

import java.io.File

import hexagon.config.HexagonConfig
import hexagon.tools.{HexagonScheduler, Logging, Pool, SysTime}

import scala.collection.Seq


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
                                  private val scheduler: HexagonScheduler,
                                  private val time: Long,
                                  needRecovery: Boolean,
                                  val logCleanupIntervalMs: Long) extends Logging {

  private val logDir = LogManager.getOrCreateLogDir(config.logDir)
  private val lock = new Object

  /**
    * logs pool
    */
  private val logs = new Pool[String, Log]()


  def startup(): Unit = {


  }


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


  def cleanupLogs(): Unit = {
    debug("Beginning logs cleanup ..")
    var total = 0
    val startMs = SysTime.mills
    val itr = getLogIterator()
    while (itr.hasNext) {
      val log = itr.next()
      debug(s"Garbage collecting '${log.name}'")
      total = cleanupExpiredSegments(log) + cleanupSegmentsToMaintainSize(log)
    }
    debug(s"Log cleanup completed. $total files deleted in ${SysTime.elapsed(startMs) / 1000} seconds")
  }


  private def getLogIterator(): Iterator[Log] = {
    logs.values().iterator
  }


  /**
    * 清理过期Segment，指文件最后一次修改时间与系统时间之差超过了配置上限
    */
  private def cleanupExpiredSegments(log: Log): Int = {
    val startMs = time
    val logCleanupThresholdMS = config.logRetentionMs
    val toBeDeleted = log.markDeleted(startMs - _.file.lastModified > logCleanupThresholdMS)
    val total = deleteSegments(log, toBeDeleted)
    total
  }

  /**
    * 检查日志大小是否超过上限，若超过则进行清理
    */
  private def cleanupSegmentsToMaintainSize(log: Log): Int = {
    val maxLogRetentionSize = config.logRetentionSize
    if (maxLogRetentionSize < 0 || log.size < maxLogRetentionSize) return 0
    var diff = log.size - maxLogRetentionSize

    def shouldDelete(segment: LogSegment) = {
      if (diff - segment.size >= 0) {
        diff -= segment.size
        true
      } else {
        false
      }
    }

    val toBeDeleted = log.markDeleted(shouldDelete)
    val total = deleteSegments(log, toBeDeleted)
    total
  }


  /**
    *
    * 删除LogSegment
    */
  private def deleteSegments(log: Log, segments: Seq[LogSegment]): Int = {
    var total = 0
    for (segment <- segments) {
      info(s"Deleting log segment ${segment.file.getName} from ${log.name}")
      swallow(segment.msgSet.close())
      //执行删除
      if (!segment.file.delete()) {
        warn("Delete failed.")
      } else {
        total += 1
      }
    }
    total
  }
}
