package hexagon.log

import java.io.{File, IOException}
import java.text.NumberFormat
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicLong}
import java.util.{ArrayList, Collections}

import hexagon.protocol.FileEntitySet
import hexagon.tools.{Logging, SysTime}


private[log] object Log {

  val FileSuffix: String = ".log"


  /**
    * 根据offset命名日志文件，名称为20个数字，无分隔符
    */
  def nameFromOffset(offset: Long): String = {
    val nf = NumberFormat.getInstance()
    nf.setMinimumIntegerDigits(20)
    nf.setMaximumFractionDigits(0)
    nf.setGroupingUsed(false)
    nf.format(offset) + FileSuffix
  }

}


/**
  * 一个topic对应的全部LogSegment集合
  */
private[log] class Log(val dir: File,
                       val time: Long,
                       val maxSize: Long,
                       val needRecovery: Boolean) extends Logging {


  private val lock = new Object

  private val unflushed = new AtomicInteger(0)

  private val lastFlushedTime = new AtomicLong(SysTime.mills)

  private[log] val segments: SegmentList = loadSegments()


  /**
    * 读取日志文件
    */
  private def loadSegments(): SegmentList = {
    val segments: ArrayList[LogSegment] = new ArrayList[LogSegment]
    // 读取已有日志文件
    val files = dir.listFiles()
    if (files != null) {
      for (file <- files if file.isFile && file.toString.endsWith(Log.FileSuffix)) {
        if (!file.canRead)
          throw new IOException("Could not read file " + file)
        val filename = file.getName()
        val start = filename.substring(0, filename.length - Log.FileSuffix.length).toLong
        val set = new FileEntitySet(file, false)
        segments.add(new LogSegment(file, time, set, start))
      }
    }

    if (segments.size == 0) {
      // 如果日志文件不存在，则从offset 0开始创建
      val newFile = new File(dir, Log.nameFromOffset(0))
      val set = new FileEntitySet(newFile, true)
      segments.add(new LogSegment(newFile, time, set, 0))
    } else {
      // 按时间对LogSegment排序
      Collections.sort(segments, (s1: LogSegment, s2: LogSegment) => {
        if (s1.start == s2.start) 0
        else if (s1.start < s2.start) -1
        else 1
      })
      validateSegments(segments)
      // 将最新的Segment设置为可写
      val last = segments.remove(segments.size - 1)
      last.entitySet.close()
      info(s"Loading the last segment ${last.file.getAbsolutePath} in mutable mode, recovery $needRecovery")
      val mutable = new LogSegment(last.file, time, new FileEntitySet(last.file, true, new AtomicBoolean(needRecovery)), last.start)
      segments.add(mutable)
    }
    //
    new SegmentList(segments.toArray(new Array[LogSegment](segments.size)))
  }

  /**
    * 校验日志文件是否连续
    */
  private def validateSegments(segments: ArrayList[LogSegment]) {
    lock synchronized {
      for (i <- 0 until segments.size - 1) {
        val curr = segments.get(i)
        val next = segments.get(i + 1)
        if (curr.start + curr.size != next.start)
          throw new IllegalStateException(s"The following segments don't validate: ${curr.file.getAbsolutePath}, ${next.file.getAbsolutePath}")
      }
    }
  }

  /**
    * 每个topic的日志文件数量
    */
  def numberOfSegments: Int = segments.view.length


  def close() {
    lock synchronized {
      for (seg <- segments.view)
        seg.entitySet.close()
    }
  }


}
