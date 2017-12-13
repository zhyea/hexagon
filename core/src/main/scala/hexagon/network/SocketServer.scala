package hexagon.network

import java.nio.channels.{SelectableChannel, SelectionKey, Selector, SocketChannel}
import java.sql.Time
import java.util.concurrent.{ConcurrentLinkedQueue, CountDownLatch}
import java.util.concurrent.atomic.AtomicBoolean

import hexagon.network.Handler.HandlerMapping
import hexagon.utils.Logging

class SocketServer(val port: Int,
                   val numProcessorThreads: Int,
                   private val handlerFactory: HandlerMapping,
                   val sendBufferSize: Int,
                   val receiveBufferSize: Int,
                   val maxRequestSize: Int = Int.MaxValue
                  ) {


}


private[network] abstract class AbstractServerThread extends Runnable with Logging {

  protected val selector: Selector = Selector.open()
  private val startupLatch = new CountDownLatch(1)
  private val shutdownLatch = new CountDownLatch(1)
  private val alive = new AtomicBoolean(false)


  def shutdown(): Unit = {
    alive.set(false)
    selector.wakeup()
    shutdownLatch.await()
  }

  def awaitStartup(): Unit = startupLatch.await()


  protected def startupComplete(): Unit = {
    alive.set(true)
    startupLatch.countDown()
  }

  protected def shutdownComplete() = shutdownLatch.countDown()


  protected def isRunning = alive.get()
}


private[network] class Processor(val handlerMapping: HandlerMapping,
                                 val time: Time,
                                 val maxRequestSize: Int) extends AbstractServerThread {

  private val newConnections = new ConcurrentLinkedQueue[SocketChannel]()

  override def run(): Unit = {
    configureNewConnections()
    while (isRunning) {

    }
  }


  private def configureNewConnections() {
    while (newConnections.size > 0) {
      val channel = newConnections.poll()
      debug("Listening to new connection from " + channel.socket.getRemoteSocketAddress)
      channel.register(selector, SelectionKey.OP_READ)
    }
  }
}