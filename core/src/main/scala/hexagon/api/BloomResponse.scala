package hexagon.api


import hexagon.utils.IOKit
import io.vertx.core.buffer.Buffer


object BloomResponse {

	def readFrom(buffer: Buffer): BloomResponse = {
		val topic = IOKit.readShortString(buffer)
		val result: Byte = buffer.getByte(0)
		new BloomResponse(topic, result)
	}
}


case class BloomResponse(topic: String, result: Byte) extends Transmission {


	override def sizeInBytes(): Int = {
		java.lang.Short.BYTES + /* topic 长度 */
			topic.length + /* topic内容 */
			1 /* 结果 */
	}


	override def writeTo(buffer: Buffer): Unit = {
		IOKit.writeShortString(buffer, topic)
		buffer.appendByte(result)
	}

	override def toString: String = s"BloomResponse(${topic}, $result)"

	override def hashCode(): Int = 31 + topic.hashCode + result

	override def equals(other: Any): Boolean = {
		other match {
			case that: BloomResponse =>
				(that canEqual this) && topic == that.topic && result == that.result
			case _ => false
		}
	}


	def canEqual(other: Any): Boolean = other.isInstanceOf[BloomResponse]


}
