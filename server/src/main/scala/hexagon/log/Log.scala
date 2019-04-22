package hexagon.log

import java.io.{File, IOException}
import java.text.NumberFormat
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicLong}
import java.util.{ArrayList, Collections}

import hexagon.protocol.FileMessageSet
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
private[log] class Log(val dir: File, // log文件目录
                       val time: Long, //
                       val maxSize: Long, // 日志文件大小上限
                       val needRecovery: Boolean) extends Logging {


  private val lock = new Object

  private val unflushed = new AtomicInteger(0)

  private val lastFlushedTime = new AtomicLong(SysTime.mills)

  private[log] val segments: SegmentList = loadSegments()

  val name: String = dir.getName


  import Log._


  def getLastFlushedTime: Long = lastFlushedTime.get()


  /**
    * 获取日志文件
    */
  private def loadSegments(): SegmentList = {
    val segments: ArrayList[LogSegment] = new ArrayList[LogSegment]
    // 读取已有日志文件
    val files = dir.listFiles()
    if (files != null) {
      for (file <- files if file.isFile && file.toString.endsWith(Log.FileSuffix)) {
        if (!file.canRead)
          throw new IOException("Could not read file " + file)
        val filename = file.getName
        val start = filename.substring(0, filename.length - Log.FileSuffix.length).toLong
        val set = new FileMessageSet(file, false)
        segments.add(new LogSegment(file, time, set, start))
      }
    }

    if (segments.size == 0) {
      // 如果日志文件不存在，则从offset 0开始创建
      val newFile = new File(dir, Log.nameFromOffset(0))
      val set = new FileMessageSet(newFile, true)
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
      last.msgSet.close()
      info(s"Loading the last segment ${last.file.getAbsolutePath} in mutable mode, recovery $needRecovery")
      val mutable = new LogSegment(last.file, time, new FileMessageSet(last.file, true, new AtomicBoolean(needRecovery)), last.start)
      segments.add(mutable)
    }
    //
    new SegmentList(segments.toArray(new Array[LogSegment](segments.size)))
  }


  /**
    * 日志整体规模
    */
  def size: Long = segments.size


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


  /**
    * 标记删除
    */
  def markDeleted(predicate: LogSegment => Boolean): Seq[LogSegment] = {
    lock synchronized {
      val view = segments.view
      val deletable = view.takeWhile(predicate)
      for (seg <- deletable) {
        seg.deletable.compareAndSet(false, true)
      }

      var numToDelete = deletable.length

      //如果要删除全部LogSegment，就还需要一个空LogSegment补上位置
      if (numToDelete == view.length) {
        val last = segments.view.last
        if (last.size > 0) {
          roll()
        } else {
          // 如果最后一个文件本来就是空的，就没必要将之删除
          last.file.setLastModified(SysTime.mills)
          numToDelete -= 1
        }
      }
      segments.truncate(numToDelete)
    }
  }


  /**
    * 创建一个新LogSegment，并添加到Log集合中
    */
  def roll(): Unit = {
    lock synchronized {
      val newOffset = nextAppendOffset()
      val newFile = new File(dir, nameFromOffset(newOffset))
      if (newFile.exists()) {
        // 如果文件已存在，将之删除
        warn(s"newly rolled log segment ${newFile.getName} already exists; deleting it first")
        newFile.delete()
      }
      debug(s"Rolling log '$name' to ${newFile.getName}")
      segments.append(new LogSegment(newFile, time, new FileMessageSet(newFile, true), newOffset))
    }
  }


  /**
    * 下一个Segment的起始offset
    */
  def nextAppendOffset(): Long = {
    flush()
    val last = segments.view.last
    last.start + last.size
  }


  /**
    * flush操作
    */
  def flush(): Unit = {
    if (unflushed.get() > 0) {
      lock synchronized {
        debug(s"Flushing log '$name', last flushed: ${lastFlushedTime.get()}, current time: ${SysTime.mills}")
        segments.view.last.msgSet.flush()
        unflushed.set(0)
        lastFlushedTime.set(SysTime.mills)
      }
    }
  }


  /**
    * 关闭资源
    */
  def close(): Unit = {
    lock synchronized {
      for (seg <- segments.view)
        seg.msgSet.close()
    }
  }


}
