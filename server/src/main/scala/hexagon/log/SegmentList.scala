package hexagon.log

import java.util.concurrent.atomic.AtomicReference
import scala.math._

private[log] class SegmentList(seq: Seq[LogSegment]) {

  val contents: AtomicReference[Array[LogSegment]] = new AtomicReference[Array[LogSegment]](seq.toArray)

  def append(ts: LogSegment*): Unit = {
    while (true) {
      val curr = contents.get()
      val updated = new Array[LogSegment](curr.length + ts.length)
      Array.copy(curr, 0, updated, 0, curr.length)
      for (i <- 0 until ts.length)
        updated(curr.length + i) = ts(i)
      if (contents.compareAndSet(curr, updated))
        return
    }
  }


  def trunc(count: Int): Seq[LogSegment] = {
    if (count < 0)
      throw new IllegalArgumentException("Starting index must be positive.")

    var deleted: Array[LogSegment] = null

    var done = false
    while (!done) {
      val curr = contents.get()
      val newLength = max(curr.length - count, 0)
      val updated = new Array[LogSegment](newLength)
      Array.copy(curr, min(count, curr.length - 1), updated, 0, newLength)
      if (contents.compareAndSet(curr, updated)) {
        deleted = new Array[LogSegment](count)
        Array.copy(curr, 0, deleted, 0, curr.length - newLength)
        done = true
      }
    }
    deleted
  }


  def view: Array[LogSegment] = contents.get()

  override def toString: String = view.toString
}
