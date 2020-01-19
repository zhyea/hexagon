package hexagon.log

import java.io.File

import hexagon.protocol.{ByteBufferMessageSet, FileMessageSet}


private[log] class LogSegment(val file: File, val time: Long, val entitySet: FileMessageSet, val start: Long) {

	var firstAppendTime: Option[Long] = None

	def size: Long = entitySet.highWaterMark()

	private def updateFirstAppendTime(): Unit = {
		if (firstAppendTime.isEmpty)
			firstAppendTime = Some(time)
	}

	def append(entities: ByteBufferMessageSet): Unit = {
		if (entities.sizeInBytes > 0) {
			entitySet.append(entities)
			updateFirstAppendTime()
		}
	}

	override def toString: String = s"(file=$file, start=$start, size=$size)"
}
