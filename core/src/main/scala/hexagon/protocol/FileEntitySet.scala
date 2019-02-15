package hexagon.protocol

import java.nio.ByteBuffer
import java.nio.channels.{FileChannel, GatheringByteChannel}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}

import hexagon.tools.SysTime

class FileEntitySet(private[protocol] val channel: FileChannel,
                    private[protocol] val offset: Long,
                    private[protocol] val limit: Long,
                    val mutable: Boolean = false,
                    val needRecover: AtomicBoolean = new AtomicBoolean(false)
                   ) extends EntitySet {

  private val setSize = new AtomicLong()
  private val setHighWaterMark = new AtomicLong()

  if (mutable) {
    if (limit < Long.MaxValue || offset > 0)
      throw new IllegalArgumentException("Attempt to open a mutable entity set with a view or offset, which is not allowed.")

    if (needRecover.get) {
      // set the file position to the end of the file for appending messages
      val start = SysTime.mills
      val truncated = recover()
      info(s"Recovery succeeded in  ${SysTime.diff(start) / 1000} seconds. $truncated bytes truncated.")
    } else {
      setSize.set(channel.size())
      setHighWaterMark.set(sizeInBytes)
      channel.position(channel.size)
    }
  } else {
    setSize.set(scala.math.min(channel.size(), limit) - offset)
    setHighWaterMark.set(sizeInBytes)
    debug("initializing high water mark in immutable mode: " + highWaterMark)
  }


  def highWaterMark(): Long = setHighWaterMark.get()


  def recover(): Long = {
    checkMutable()
    val len = channel.size
    val buffer = ByteBuffer.allocate(4)
    var validUpTo: Long = 0
    var next = 0L
    do {
      next = validateMessage(channel, validUpTo, len, buffer)
      if (next >= 0)
        validUpTo = next
    } while (next >= 0)
    channel.truncate(validUpTo)
    setSize.set(validUpTo)
    setHighWaterMark.set(validUpTo)
    info("recover high water mark:" + highWaterMark)

    channel.position(validUpTo)
    needRecover.set(false)
    len - validUpTo
  }

  def checkMutable(): Unit = {
    if (!mutable)
      throw new IllegalStateException("Attempt to invoke mutation on immutable entity set.")
  }


  private def validateMessage(channel: FileChannel, start: Long, len: Long, buffer: ByteBuffer): Long = {
    buffer.rewind()
    var read = channel.read(buffer, start)
    if (read < 4)
      return -1

    // check that we have sufficient bytes left in the file
    val size = buffer.getInt(0)
    if (size < Entity.headerSize())
      return -1

    val next = start + 4 + size
    if (next > len)
      return -1

    // read the message
    val entityBuffer = ByteBuffer.allocate(size)
    var curr = start + 4
    while (entityBuffer.hasRemaining) {
      read = channel.read(entityBuffer, curr)
      if (read < 0)
        throw new IllegalStateException("File size changed during recovery!")
      else
        curr += read
    }
    entityBuffer.rewind()
    val message = new Entity(entityBuffer)
    if (!message.isValid)
      return -1
    else
      next
  }

  override def writeTo(channel: GatheringByteChannel, offset: Long, maxSize: Long): Long = ???

  override def iterator: Iterator[EntityAndOffset] = ???

  override def sizeInBytes: Int = ???
}
