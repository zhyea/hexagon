package hexagon.network

import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer

/**
 * 读取msg
 *
 * @author robin
 */
class BufferHandler extends Handler[Buffer] {


	override def handle(buff: Buffer): Unit = {
		val bytes = buff.getBytes
		bytes.foreach(println)
	}


}
