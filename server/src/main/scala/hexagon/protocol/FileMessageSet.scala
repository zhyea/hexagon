package hexagon.protocol

import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.{FileChannel, GatheringByteChannel}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}

import hexagon.tools.{BYTES, SysTime}
import hexagon.utils.IOUtils

class FileMessageSet(private[protocol] val channel: FileChannel, // 文件channel
                     private[protocol] val offset: Long, // 日志中所有消息的起始offset
                     private[protocol] val limit: Long, //
                     val mutable: Boolean = false, // 日志是否允许修改
                     val needRecover: AtomicBoolean = new AtomicBoolean(false)
                    ) extends MessageSet {

  private val setSize = new AtomicLong()
  private val setHighWaterMark = new AtomicLong()

  if (mutable) {
    if (limit < Long.MaxValue || offset > 0)
      throw new IllegalArgumentException("Attempt to open a mutable entity set with a view or offset, which is not allowed.")

    if (needRecover.get) {
      // set the file position to the end of the file for appending messages
      val start = SysTime.mills
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
    val buffer = ByteBuffer.allocate(BYTES.Int)
    var validUpTo: Long = 0
    var next = 0L
    // 循环校验日志文件中每一条消息的有效性
    do {
      next = validate(channel, validUpTo, len, buffer)
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


  /**
    * 检验当前日志文件是否允许修改。需要在每一次修改执行之前调用
    */
  def checkMutable(): Unit = {
    if (!mutable)
      throw new IllegalStateException("Attempt to invoke mutation on immutable entity set.")
  }

  /**
    * 校验文件中消息的有效性
    *
    * @param channel 日志文件Channel
    * @param start   当前消息在文件中的起始位置
    * @param len     日志文件总长度
    * @param buffer  存储消息长度的buffer
    * @return 获取下一个消息的起始位置
    */
  private def validate(channel: FileChannel, start: Long, len: Long, buffer: ByteBuffer): Long = {
    buffer.rewind()
    // 读取消息长度
    var read = channel.read(buffer, start)
    if (read < BYTES.Int)
      return -1

    // 检验消息整体长度是否大于消息头长度
    val size = buffer.getInt(0)
    if (size < Message.headerSize())
      return -1
    // 检验文件剩余空间是否充足
    val next = start + 4 + size
    if (next > len)
      return -1

    // 读取消息到buffer
    val msgBuffer = ByteBuffer.allocate(size)
    var curr = start + BYTES.Int
    while (msgBuffer.hasRemaining) {
      read = channel.read(msgBuffer, curr)
      if (read < 0)
        throw new IllegalStateException("File size changed during recovery!")
      else
        curr += read
    }
    msgBuffer.rewind()

    // 校验消息是否有效
    val msg = new Message(msgBuffer)
    if (!msg.isValid)
      -1
    else
      next
  }

  def read(readOffset: Long, size: Long): MessageSet = {
    new FileMessageSet(channel, this.offset + readOffset, scala.math.min(this.offset + readOffset + size, highWaterMark),
      false, new AtomicBoolean(false))
  }


  /**
    * 将数据写入另一个Channel
    */
  override def writeTo(destChannel: GatheringByteChannel, writeOffset: Long, size: Long): Long = {
    // 利用了zero copy的性能优势
    channel.transferTo(offset + writeOffset, scala.math.min(size, sizeInBytes), destChannel)
  }


  /**
    * 追加消息
    */
  def append(msgs: MessageSet): Unit = {
    checkMutable()
    var written = 0L
    while (written < msgs.sizeInBytes)
      written += msgs.writeTo(channel, 0, msgs.sizeInBytes)
    setSize.getAndAdd(written)
  }


  def flush(): Unit = {
    checkMutable()
    channel.force(true)
    setHighWaterMark.set(sizeInBytes)
    debug("flush high water mark:" + highWaterMark)
  }


  def close(): Unit = {
    if (mutable)
      flush()
    channel.close()
  }


  override def iterator: Iterator[MessageAndOffset] = ???

  override def sizeInBytes: Long = setSize.get()
}
