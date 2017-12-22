package hexagon.server

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

import hexagon.config.HexagonConfig
import hexagon.network.SocketServer
import hexagon.utils.Logging

class HexagonServer(val config: HexagonConfig) extends Logging {

	private val isShutdown: AtomicBoolean = new AtomicBoolean(false)
	private val shutdownLatch = new CountDownLatch(1)

	var socketServer: SocketServer = null


	def startup(): Unit = {

	}


}
