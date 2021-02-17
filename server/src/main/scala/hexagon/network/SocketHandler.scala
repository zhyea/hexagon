package hexagon.network

import io.vertx.core.Handler
import io.vertx.core.net.NetSocket

/**
 *
 * @author robin
 */
class SocketHandler extends Handler[NetSocket] {


	private val msgHandler = new BufferHandler()


	override def handle(socket: NetSocket): Unit = {
		socket.handler(msgHandler)
	}


}
