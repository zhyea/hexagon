package hexagon.server

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import hexagon.config.HexagonConfig
import hexagon.network
import hexagon.tools.Logging
import io.vertx.core.{Vertx, VertxOptions}
import io.vertx.core.net.{NetServer, NetServerOptions}

class HexagonServer(val config: HexagonConfig) extends Logging {

	private val isRunning: AtomicBoolean = new AtomicBoolean(false)
	private val shutdownLatch: CountDownLatch = new CountDownLatch(1)

	private var netServer: NetServer = _

	def startup(): Unit = {
		info("Hexagon server is starting.")
		isRunning.set(true)


		val vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(config.numNetworkThreads))

		val options =
			new NetServerOptions()
				.setPort(config.port)
				.setReceiveBufferSize(config.socketReceiveBuffer)
				.setSendBufferSize(config.socketSendBuffer)


		netServer = vertx.createNetServer(options)


		netServer.startup()

		info("Hexagon server started")
	}


	def awaitShutdown(): Unit = shutdownLatch.await()

	def shutdown(): Unit = {
		val canShutdown = isRunning.compareAndSet(true, false)
		if (canShutdown) {
			info("Shutting down hexagon server.")
			if (null != netServer) netServer.shutdown()
			shutdownLatch.countDown()
			info("Shutdown hexagon server completely.")
		}
	}

}
