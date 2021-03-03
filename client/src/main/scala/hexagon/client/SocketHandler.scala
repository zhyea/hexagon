package hexagon.client

import hexagon.api.BloomRequest
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetSocket

/**
 *
 * @author robin
 */
class SocketHandler extends Handler[NetSocket] {


	override def handle(socket: NetSocket): Unit = {

		socket.handler(buff => {
			val request = BloomRequest.readFrom(buff)

			println(request.message)

			val out = Buffer.buffer()
			out.appendByte(1)
			socket.write(out)
		})

	}

}
