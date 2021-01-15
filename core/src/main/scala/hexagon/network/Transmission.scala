package hexagon.network

import java.nio.ByteBuffer
import java.nio.channels.{GatheringByteChannel, ReadableByteChannel}
import java.util.concurrent.atomic.AtomicBoolean

import hexagon.exceptions.HexagonException
import hexagon.tools.Logging

private[hexagon] trait Transmission extends Logging {

	val complete: AtomicBoolean = new AtomicBoolean(false)

	final def isCompleted: Boolean = complete.get()

	final def expectComplete(): Unit =
		if (!isCompleted) throw new HexagonException("This operation cannot be completed on an incomplete request.")


	final def expectIncomplete(): Unit =
		if (isCompleted) throw new HexagonException("This operation cannot be completed on a complete request.")


}


trait Receive extends Transmission {

	def buffer: ByteBuffer

	def readFrom(channel: ReadableByteChannel): Int

	def readCompletely(channel: ReadableByteChannel): Int = {
		var totalRead = 0
		while (!isCompleted) {
			totalRead += readFrom(channel)
		}
		totalRead
	}

}


trait Send extends Transmission {

	def writeTo(channel: GatheringByteChannel): Int

	def writeCompletely(channel: GatheringByteChannel): Int = {
		var totalWrite = 0
		while (!isCompleted) {
			totalWrite += writeTo(channel)
		}
		totalWrite
	}

}