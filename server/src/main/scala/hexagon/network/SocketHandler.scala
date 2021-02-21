package hexagon.network

import io.vertx.core.Handler
import io.vertx.core.net.NetSocket

/**
 *
 * @author robin
 */
class SocketHandler extends Handler[NetSocket] {


	override def handle(socket: NetSocket): Unit = {

		socket.handler(buff => {
			val bytes = buff.getBytes
			bytes.foreach(println)


			buff
		})

	}
	
}
