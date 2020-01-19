package hexagon.protocol

import java.nio.ByteBuffer
import java.nio.channels.GatheringByteChannel

import hexagon.exceptions.InvalidMessageException
import hexagon.tools.Logging


object MessageSet {

	def messageSetSize(messages: Iterable[Message]): Int = messages.foldLeft(0)(_ + _.serializedSize)


	def createByteBuffer(messages: Message*): ByteBuffer = {
		val buffer = ByteBuffer.allocate(messageSetSize(messages))
		for (msg <- messages) {
			msg.serializeTo(buffer)
		}
		buffer.rewind
		buffer
	}

}


/**
  * 消息实例集合，主要用来从日志中读取消息实例
  */
abstract class MessageSet extends Iterable[MessageAndOffset] with Logging {

	/**
	  * 将消息写入指定的channel
	  */
	def writeTo(channel: GatheringByteChannel, offset: Long, maxSize: Long): Long

	/**
	  * 迭代器
	  */
	def iterator: Iterator[MessageAndOffset]

	/**
	  * 转为Byte数组后的长度
	  */
	def sizeInBytes: Long

	/**
	  * 判断是否可以进行equal比较
	  */
	def canEqual(other: Any): Boolean

	/**
	  * 校验集合中的每个元素是否有效
	  */
	def validate(): Unit = {
		for (messageAndOffset <- this)
			if (!messageAndOffset.message.isValid)
				throw new InvalidMessageException()
	}

}
