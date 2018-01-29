package hexagon.server

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

import hexagon.config.HexagonConfig
import hexagon.network.SocketServer

class HexagonServer(val config: HexagonConfig) {

  private val isRunning: AtomicBoolean = new AtomicBoolean(false)
  private val shutdownLatch: CountDownLatch = new CountDownLatch(1)
  private var socketServer: SocketServer = null

}
