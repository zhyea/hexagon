package hexagon.protocol

import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.{FileChannel, GatheringByteChannel}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}

import hexagon.tools.SysTime
import hexagon.utils.IOUtils

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
      val start = SysTime.milli
      val truncated = recover()
      info(s"Recovery succeeded in  ${SysTime.elapsed(start) / 1000} seconds. $truncated bytes truncated.")
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


  def this(channel: FileChannel, mutable: Boolean) =
    this(channel, 0, Long.MaxValue, mutable, new AtomicBoolean(false))


  def this(file: File, mutable: Boolean) =
    this(IOUtils.openChannel(file, mutable), mutable)


  def this(channel: FileChannel, mutable: Boolean, needRecover: AtomicBoolean) =
    this(channel, 0, Long.MaxValue, mutable, needRecover)


  def this(file: File, mutable: Boolean, needRecover: AtomicBoolean) =
    this(IOUtils.openChannel(file, mutable), mutable, needRecover)


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
      -1
    else
      next
  }

  def read(readOffset: Long, size: Long): EntitySet = {
    new FileEntitySet(channel, this.offset + readOffset, scala.math.min(this.offset + readOffset + size, highWaterMark),
      false, new AtomicBoolean(false))
  }

  override def writeTo(destChannel: GatheringByteChannel, writeOffset: Long, size: Long): Long = {
    channel.transferTo(offset + writeOffset, scala.math.min(size, sizeInBytes), destChannel)
  }


  def append(entities: EntitySet): Unit = {
    checkMutable()
    var written = 0L
    while (written < entities.sizeInBytes)
      written += entities.writeTo(channel, 0, entities.sizeInBytes)
    setSize.getAndAdd(written)
  }


  def flush() = {
    checkMutable()
    channel.force(true)
    setHighWaterMark.set(sizeInBytes)
    debug("flush high water mark:" + highWaterMark)
  }


  def close() = {
    if (mutable)
      flush()
    channel.close()
  }


  override def iterator: Iterator[EntityAndOffset] = ???

  override def sizeInBytes: Long = setSize.get()

  override def canEqual(other: Any): Boolean = ???
}
