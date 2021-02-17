package hexagon.server

import hexagon.config.HexagonConfig
import hexagon.network.ServerVerticle
import hexagon.tools.Logging
import io.vertx.core.{AsyncResult, DeploymentOptions, Handler, Vertx}

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

class HexagonServer(val config: HexagonConfig) extends Logging {

	private val isRunning: AtomicBoolean = new AtomicBoolean(false)
	private val shutdownLatch: CountDownLatch = new CountDownLatch(1)
	private var vertx: Vertx = _


	def startup(): Unit = {
		info("Hexagon server is starting.")

		isRunning.set(true)

		val options: DeploymentOptions =
			new DeploymentOptions()
				.setWorkerPoolName("plumber-pool")
				.setWorkerPoolSize(1)

		vertx = Vertx.vertx()

		vertx.getOrCreateContext().put("cfg", config)

		vertx.deployVerticle(classOf[ServerVerticle].getName, options, new Handler[AsyncResult[String]] {
			override def handle(result: AsyncResult[String]): Unit = {
				if (result.succeeded) {
					System.out.println("Server is now listening!")
				} else {
					System.out.println("Failed to bind!")
				}
			}
		})

		info("Hexagon server started")
	}


	def awaitShutdown(): Unit = shutdownLatch.await()

	def shutdown(): Unit = {
		val canShutdown = isRunning.compareAndSet(true, false)
		if (canShutdown) {
			info("Shutting down hexagon server.")
			if (null != vertx) {
				vertx.close()
			}
			shutdownLatch.countDown()
			info("Shutdown hexagon server completely.")
		}
	}

}
