package hexagon.log

import java.io.{File, IOException}
import java.text.NumberFormat
import java.util.{ArrayList, Collections, Comparator}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicLong}

import hexagon.protocol.FileEntitySet
import hexagon.tools.{Logging, SysTime}


private[log] object Log {

  val FileSuffix: String = ".log"


  def nameFromOffset(offset: Long): String = {
    val nf = NumberFormat.getInstance()
    nf.setMinimumIntegerDigits(20)
    nf.setMaximumFractionDigits(0)
    nf.setGroupingUsed(false)
    nf.format(offset) + FileSuffix
  }

}


private[log] class Log(val dir: File, val time: Long, val maxSize: Long, val maxMessageSize: Int,
                       val flushInterval: Int, val rollIntervalMs: Long, val needRecovery: Boolean) extends Logging {


  private val lock = new Object

  private val unflushed = new AtomicInteger(0)

  private val lastFlushedTime = new AtomicLong(SysTime.mills)

  private[log] val segments: SegmentList[LogSegment] = loadSegments()


  private def loadSegments(): SegmentList[LogSegment] = {
    val accum: ArrayList[LogSegment] = new ArrayList[LogSegment]
    val ls = dir.listFiles()
    if (ls != null) {
      for (file <- ls if file.isFile && file.toString.endsWith(Log.FileSuffix)) {
        if (!file.canRead)
          throw new IOException("Could not read file " + file)
        val filename = file.getName()
        val start = filename.substring(0, filename.length - Log.FileSuffix.length).toLong
        val messageSet = new FileEntitySet(file, false)
        accum.add(new LogSegment(file, time, messageSet, start))
      }
    }

    if (accum.size == 0) {
      val newFile = new File(dir, Log.nameFromOffset(0))
      val set = new FileEntitySet(newFile, true)
      accum.add(new LogSegment(newFile, time, set, 0))
    } else {
      Collections.sort(accum, new Comparator[LogSegment] {
        def compare(s1: LogSegment, s2: LogSegment): Int = {
          if (s1.start == s2.start) 0
          else if (s1.start < s2.start) -1
          else 1
        }
      })
      validateSegments(accum)

      val last = accum.remove(accum.size - 1)
      last.entitySet.close()
      info(s"Loading the last segment ${last.file.getAbsolutePath} in mutable mode, recovery $needRecovery")
      val mutable = new LogSegment(last.file, time, new FileEntitySet(last.file, true, new AtomicBoolean(needRecovery)), last.start)
      accum.add(mutable)
    }
    new SegmentList(accum.toArray(new Array[LogSegment](accum.size)))
  }


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


}
