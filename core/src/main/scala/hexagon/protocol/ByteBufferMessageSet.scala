package hexagon.protocol

import java.nio.ByteBuffer
import java.nio.channels.GatheringByteChannel

import hexagon.exceptions.{InvalidMessageException, InvalidMessageSizeException}
import hexagon.tools.ItrTemplate

class ByteBufferMessageSet(private val buffer: ByteBuffer,
						   private val initOffset: Long = 0L) extends MessageSet {


	def this(messages: Message*) = this(MessageSet.createByteBuffer(messages: _*))


	def getBuffer: ByteBuffer = buffer


	def getInitOffset: Long = initOffset


	def serialized: ByteBuffer = buffer


	override def writeTo(channel: GatheringByteChannel, offset: Long, maxSize: Long): Long = {
		buffer.mark()
		val written = channel.write(buffer)
		buffer.reset()
		written
	}

	override def sizeInBytes: Long = buffer.limit()


	override def toString: String = {
		val builder = new StringBuilder()
		builder.append("ByteBufferMessageSet(")
		for (entity <- this) {
			builder.append(entity)
			builder.append(", ")
		}
		builder.append(")")
		builder.toString
	}

	override def equals(other: Any): Boolean = {
		other match {
			case that: ByteBufferMessageSet =>
				(that canEqual this) && buffer.equals(that.buffer) && initOffset == that.initOffset
			case _ => false
		}
	}

	override def canEqual(other: Any): Boolean = other.isInstanceOf[ByteBufferMessageSet]

	override def hashCode: Int = 31 + buffer.hashCode + initOffset.hashCode

	override def iterator: Iterator[MessageAndOffset] = new Itr()

	def shallowIterator: Iterator[MessageAndOffset] = new Itr(true)

	def verifyMessageSize(maxMessageSize: Int): Unit = {
		for (e <- shallowIterator) {
			val payloadSize = e.message.payloadSize()
			if (payloadSize > maxMessageSize)
				throw new MessageSizeTooLargeException(payloadSize, maxMessageSize)
		}
	}

	private class Itr(isShallow: Boolean = false) extends ItrTemplate[MessageAndOffset] {

		var itrBuffer: ByteBuffer = buffer.slice()
		var currValidBytes: Long = initOffset
		var lastMessageSize: Long = 0L

		override protected def makeNext(): MessageAndOffset = {
			if (itrBuffer.remaining() < Integer.BYTES) {
				return done()
			}

			val size = itrBuffer.getInt
			lastMessageSize = size

			trace(s"Remaining bytes in iterator = ${itrBuffer.remaining()}")
			trace(s"Size of data = $size")

			if (size < 0 || size > itrBuffer.remaining()) {
				if (size < 0 || currValidBytes == initOffset) {
					throw new InvalidMessageSizeException(
						s"""
						   |Invalid message size: $size, only received bytes: ${itrBuffer.remaining()} at $currValidBytes.
						   |Possible causes: (1) a single message larger than the fetch size; (2) log corruption.
             """.stripMargin
					)
				}
				return done()
			}

			val buffer = itrBuffer.slice()
			buffer.limit(size)
			itrBuffer.position(itrBuffer.position() + size)

			val newEntity = new Message(buffer)
			if (!newEntity.isValid) {
				throw new InvalidMessageException(s"Message is invalid, compression codec: size: $size, curr offset: $currValidBytes, init offset:$initOffset. ")
			}

			debug(s"Valid byte count = $currValidBytes")
			currValidBytes += 4 + size
			trace(s"currValidBytes = $currValidBytes")
			MessageAndOffset(newEntity, currValidBytes)
		}
	}

}
