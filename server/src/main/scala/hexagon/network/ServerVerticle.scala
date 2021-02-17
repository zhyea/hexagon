package hexagon.network

import hexagon.config.HexagonConfig
import io.vertx.core.net.{NetServer, NetServerOptions}
import io.vertx.core.{AbstractVerticle, Promise}


/**
 * Server
 *
 * @author robin
 */
class ServerVerticle extends AbstractVerticle {


	private var server: NetServer = _

	private val socketHandler: SocketHandler = new SocketHandler()

	override def start(promise: Promise[Void]): Unit = {

		val config: HexagonConfig = context.get("cfg")

		val option: NetServerOptions =
			new NetServerOptions()
				.setPort(config.port)
				.setReceiveBufferSize(config.socketReceiveBuffer)
				.setSendBufferSize(config.socketSendBuffer)

		server = vertx.createNetServer(option)
			.connectHandler(socketHandler)

		server.listen()
	}


}
