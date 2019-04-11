package hexagon.server

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

import hexagon.bloom.BloomFilterManager
import hexagon.config.HexagonConfig
import hexagon.handler.RequestHandlers
import hexagon.network.SocketServer
import hexagon.tools.Logging

class HexagonServer(val config: HexagonConfig) extends Logging {

  private val isRunning: AtomicBoolean = new AtomicBoolean(false)
  private val shutdownLatch: CountDownLatch = new CountDownLatch(1)

  private var socketServer: SocketServer = _

  private var bloomFilterManager: BloomFilterManager = _

  def startup(): Unit = {
    info("Hexagon server is starting.")
    isRunning.set(true)

    bloomFilterManager = new BloomFilterManager(config)

    val handlers = new RequestHandlers(bloomFilterManager)

    socketServer = new SocketServer(config.host,
      config.port,
      handlers,
      config.socketSendBuffer,
      config.socketReceiveBuffer,
      config.maxSocketRequestSize)

    socketServer.startup()

    info("Hexagon server started")
  }


  def awaitShutdown(): Unit = shutdownLatch.await()

  def shutdown(): Unit = {
    val canShutdown = isRunning.compareAndSet(true, false)
    if (canShutdown) {
      info("Shutting down hexagon server.")

      if (null != socketServer)
        socketServer.shutdown()

      if (null != bloomFilterManager)
        bloomFilterManager.close()

      shutdownLatch.countDown()
      info("Shutdown hexagon server completely.")
    }

  }

}
