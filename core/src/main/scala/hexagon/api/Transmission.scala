package hexagon.api

import io.vertx.core.buffer.Buffer

/**
 *
 * @author robin
 */
trait Transmission {

	/**
	 * 消息size
	 *
	 * @return 消息字节数组的大小
	 */
	def sizeInBytes(): Int


	/**
	 * 将消息写入Buffer
	 *
	 * @param buffer 回复的Buffer
	 */
	def writeTo(buffer: Buffer): Unit

}
