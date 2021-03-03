package hexagon.client

import hexagon.config.HexagonClientConfig
import io.vertx.core.net.{NetClient, NetClientOptions}
import io.vertx.core.{AbstractVerticle, Promise}

/**
 *
 * @author robin
 */
class ClientVerticle(config: HexagonClientConfig) extends AbstractVerticle {

	private var client: NetClient = _


	override def start(startPromise: Promise[Void]): Unit = {
		val option: NetClientOptions =
			new NetClientOptions()

		client = vertx.createNetClient(option)
		client.connect(config.port, config.host )
	}


	override def stop(stopPromise: Promise[Void]): Unit = {

	}

}
