package hexagon.api


import hexagon.utils.IOKit
import io.vertx.core.buffer.Buffer


object BloomRequest {

	def readFrom(buffer: Buffer): BloomRequest = {
		val topic = IOKit.readShortString(buffer)
		val message = IOKit.readShortString(buffer)
		new BloomRequest(topic, message)
	}
}


case class BloomRequest(topic: String,
						message: String) extends Transmission {

	/**
	 * topicLength + topic + messageSize + message
	 */
	override def sizeInBytes(): Int = {
		java.lang.Short.BYTES + /* topic长度 */
			topic.length + /* topic */
			Integer.BYTES + /* message数量 */
			message.length /* message信息 */
	}

	override def writeTo(buffer: Buffer): Unit = {
		IOKit.writeShortString(buffer, topic)
		IOKit.writeShortString(buffer, message)
	}

	override def toString: String = s"BloomRequest(${topic}, ${message})"

	override def equals(other: Any): Boolean = {
		other match {
			case that: BloomRequest =>
				(that canEqual this) && topic == that.topic && message.equals(that.message)
			case _ => false
		}
	}

	def canEqual(other: Any): Boolean = other.isInstanceOf[BloomRequest]


	override def hashCode(): Int = 31 + topic.hashCode + message.hashCode()
}
