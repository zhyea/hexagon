package hexagon.log

import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

import hexagon.protocol.{ByteBufferEntitySet, FileMessageSet}


/**
  * 日志文件描述类，每个LogSegment对应一个日志文件，日志文件名以起始offset命名
  */
private[log] class LogSegment(val file: File, // log文件实例
                              val time: Long,
                              val msgSet: FileMessageSet,
                              val start: Long) { // 起始offset

  val deletable: AtomicBoolean = new AtomicBoolean(false)

  var firstAppendTime: Option[Long] = None

  def size: Long = msgSet.highWaterMark()

  private def updateFirstAppendTime(): Unit = {
    if (firstAppendTime.isEmpty)
      firstAppendTime = Some(time)
  }

  def append(entities: ByteBufferEntitySet): Unit = {
    if (entities.sizeInBytes > 0) {
      msgSet.append(entities)
      updateFirstAppendTime()
    }
  }

  override def toString: String = s"(file=$file, start=$start, size=$size)"
}
