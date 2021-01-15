package hexagon.api

import java.nio.ByteBuffer

import hexagon.network.RequestOrResponse
import hexagon.utils.IOUtils

import scala.collection.mutable.ListBuffer


object BloomResponse {

	def readFrom(buffer: ByteBuffer): BloomResponse = {
		val topic = IOUtils.readShortString(buffer)
		val len = buffer.getInt
		val msgStates = ListBuffer[Short]()

		for (i <- 0 to len) msgStates += buffer.getShort

		new BloomResponse(topic, msgStates.toList)
	}
}


case class BloomResponse(topic: String, msgStates: List[Short]) extends RequestOrResponse(RequestKeys.Bloom) {


	override def sizeInBytes: Int = {
		java.lang.Short.BYTES + /* topic 长度 */
			topic.length + /* topic内容 */
			Integer.BYTES + /* message数量 */
			msgStates.size * java.lang.Short.BYTES /* message 状态 */
	}


	override def writeTo(buffer: ByteBuffer): Unit = {
		IOUtils.writeShortString(buffer, topic)
		buffer.putInt(msgStates.size)
		msgStates.foreach(buffer.putShort)
	}

	override def toString: String = s"BloomResponse(${topic}, $msgStates)"

	override def hashCode(): Int = 31 + topic.hashCode + msgStates.hashCode()

	override def equals(other: Any): Boolean = {
		other match {
			case that: BloomResponse =>
				(that canEqual this) && topic == that.topic && msgStates.equals(that.msgStates)
			case _ => false
		}
	}


	def canEqual(other: Any): Boolean = other.isInstanceOf[BloomResponse]


}
