package hexagon.api

import java.nio.ByteBuffer

import hexagon.network.Request
import hexagon.protocol.ByteBufferMessageSet
import hexagon.utils.IOUtils


object PutRequest {

	def readFrom(buffer: ByteBuffer): PutRequest = {
		val topic = IOUtils.readShortString(buffer)
		val entitySetSize = buffer.getInt
		val entitySetBuffer = buffer.slice()
		entitySetBuffer.limit(entitySetSize)
		buffer.position(buffer.position() + entitySetSize)
		new PutRequest(topic, new ByteBufferMessageSet(entitySetBuffer))
	}
}


class PutRequest(val topic: String,
                 val entitySet: ByteBufferMessageSet) extends Request(RequestKeys.Put) {

	/**
	  * topicLength + topic + entitySetSize + entity
	  */
	override def sizeInBytes: Int = 2 + topic.length + 4 + entitySet.sizeInBytes.toInt

	override def writeTo(buffer: ByteBuffer): Unit = {
		IOUtils.writeShortString(buffer, topic)
		buffer.putInt(entitySet.sizeInBytes.toInt)
		buffer.put(entitySet.serialized)
		entitySet.serialized.rewind()
	}

	override def toString: String = {
		val builder = new StringBuilder()
		builder.append("PutRequest(")
		builder.append(topic + ",")
		builder.append(entitySet.sizeInBytes)
		builder.append(")")
		builder.toString
	}

	override def equals(other: Any): Boolean = {
		other match {
			case that: PutRequest =>
				(that canEqual this) && topic == that.topic && entitySet.equals(that.entitySet)
			case _ => false
		}
	}

	def canEqual(other: Any): Boolean = other.isInstanceOf[PutRequest]

	override def hashCode(): Int = 31 + topic.hashCode + entitySet.hashCode()
}
