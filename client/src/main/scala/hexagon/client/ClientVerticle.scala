package hexagon.client

import hexagon.config.HexagonClientConfig
import io.vertx.core.{AbstractVerticle, Promise}
import io.vertx.core.net.NetClient

/**
 *
 * @author robin
 */
class ClientVerticle(config: HexagonClientConfig) extends AbstractVerticle {

	private var client: NetClient = _


	override def start(startPromise: Promise[Void]): Unit = {

	}


	override def stop(stopPromise: Promise[Void]): Unit = {

	}

}
