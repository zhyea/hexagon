package hexagon.api

import java.nio.ByteBuffer
import hexagon.network.RequestOrResponse
import hexagon.protocol.ByteBufferMessageSet
import hexagon.utils.IOUtils
import io.vertx.core.buffer.Buffer


object BloomRequest {

	def readFrom(buffer:Buffer): BloomRequest = {
		val topic = IOUtils.readShortString(buffer)
		val entitySetSize = buffer.getInt(0)
		val messageSetBuffer = buffer.slice()
		messageSetBuffer.limit(entitySetSize)
		buffer.position(buffer.position() + entitySetSize)
		new BloomRequest(topic, new ByteBufferMessageSet(messageSetBuffer))
	}
}


case class BloomRequest(topic: String,
						messageSet: ByteBufferMessageSet) extends RequestOrResponse(RequestKeys.Bloom) {

	/**
	  * topicLength + topic + messageSetSize + entity
	  */
	override def sizeInBytes: Int = {
		java.lang.Short.BYTES + /* topic长度 */
			topic.length + /* topic */
			Integer.BYTES + /* message数量 */
			messageSet.sizeInBytes.toInt /* message信息 */
	}

	override def writeTo(buffer: ByteBuffer): Unit = {
		IOUtils.writeShortString(buffer, topic)
		buffer.putInt(messageSet.sizeInBytes.toInt)
		buffer.put(messageSet.serialized)
		messageSet.serialized.rewind()
	}

	override def toString: String = s"BloomRequest(${topic}, ${messageSet.sizeInBytes})"

	override def equals(other: Any): Boolean = {
		other match {
			case that: BloomRequest =>
				(that canEqual this) && topic == that.topic && messageSet.equals(that.messageSet)
			case _ => false
		}
	}

	def canEqual(other: Any): Boolean = other.isInstanceOf[BloomRequest]

	override def hashCode(): Int = 31 + topic.hashCode + messageSet.hashCode()
}
