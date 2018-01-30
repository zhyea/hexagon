package hexagon.server

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

import hexagon.config.HexagonConfig
import hexagon.network.SocketServer
import hexagon.tools.Logging

class HexagonServer(val config: HexagonConfig) extends Logging {

  private val isRunning: AtomicBoolean = new AtomicBoolean(false)
  private val shutdownLatch: CountDownLatch = new CountDownLatch(1)

  private var socketServer: SocketServer = _

  def startup(): Unit = {
    isRunning.set(true)
  }


  def awaitShutdown(): Unit = shutdownLatch.await()

  def shutdown(): Unit = {
    val canShutdown = isRunning.compareAndSet(true, false)
    if (canShutdown) {
      info("Shutting down hexagon server.")
      if (null != socketServer) socketServer.shutdown()
      shutdownLatch.countDown()
      info("Shutdown hexagon server completely.")
    }

  }

}
