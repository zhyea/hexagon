package hexagon.network

import hexagon.config.HexagonServerConfig
import io.vertx.core.net.{NetServer, NetServerOptions}
import io.vertx.core.{AbstractVerticle, Promise}


/**
 * Server
 *
 * @author robin
 */
class ServerVerticle(config: HexagonServerConfig) extends AbstractVerticle {


	private var server: NetServer = _

	private val socketHandler: SocketHandler = new SocketHandler()

	override def start(promise: Promise[Void]): Unit = {

		val option: NetServerOptions =
			new NetServerOptions()
				.setPort(config.port)
				.setReceiveBufferSize(config.socketReceiveBuffer)
				.setSendBufferSize(config.socketSendBuffer)

		server = vertx.createNetServer(option)
			.connectHandler(socketHandler)

		server.listen()
	}

	override def stop(stopPromise: Promise[Void]): Unit = {
		server.close(result => {
			if (result.succeeded()) {
				println("Close successfully!")
			}
		})
	}
}
