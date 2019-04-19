package hexagon.log

import java.io.File

import hexagon.protocol.{ByteBufferEntitySet, FileEntitySet}


/**
  * 日志文件描述类，每个LogSegment对应一个日志文件，日志文件名以起始offset命名
  */
private[log] class LogSegment(val file: File, val time: Long, val entitySet: FileEntitySet, val start: Long) {

  var firstAppendTime: Option[Long] = None

  def size: Long = entitySet.highWaterMark()

  private def updateFirstAppendTime(): Unit = {
    if (firstAppendTime.isEmpty)
      firstAppendTime = Some(time)
  }

  def append(entities: ByteBufferEntitySet): Unit = {
    if (entities.sizeInBytes > 0) {
      entitySet.append(entities)
      updateFirstAppendTime()
    }
  }

  override def toString: String = s"(file=$file, start=$start, size=$size)"
}
