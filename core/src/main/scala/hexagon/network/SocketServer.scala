package hexagon.network

import java.net.InetSocketAddress
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel}
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

import hexagon.exceptions.HexagonConnectException
import hexagon.tools.{Logging, StringUtils}


class SocketServer(private val port: Int,
                   private val numProcessorThreads: Int) {


  private var selector: Selector = null
  private var serverSocketChannel: ServerSocketChannel = null

  def start(): Unit = {
    selector = Selector.open()
    serverSocketChannel = ServerSocketChannel.open()
    serverSocketChannel.configureBlocking(false)
    serverSocketChannel.socket().bind(new InetSocketAddress(port))
    serverSocketChannel.register(selector, SelectionKey.OP_READ)

  }


  def shutdown(): Unit = {
    if (null != serverSocketChannel) serverSocketChannel.close()
    if (null != selector) selector.close()
  }

}


private abstract class AbstractServerThread() extends Runnable with Logging {

  protected val selector = Selector.open()
  private val startupLatch = new CountDownLatch(1)
  private val shutdownLatch = new CountDownLatch(1)
  private val alive = new AtomicBoolean(false)


  def awaitStartup(): Unit = startupLatch.await()


  def startupComplete(): Unit = {
    alive.set(true)
    startupLatch.countDown()
  }


  def shutdown(): Unit = {
    selector.wakeup()
    shutdownLatch.await()
    alive.set(false)
  }

  def shutdownComplete() = shutdownLatch.countDown()


  def wakeup() = selector.wakeup()


  def isRunning(): Boolean = alive.get()
}


private class ServerThread(val host: String,
                           val port: Int,
                           val sendBufferSize: Int,
                           val receiveBufferSize: Int) extends AbstractServerThread with Logging {

  val serverSocketChannel = openSocket()

  override def run(): Unit = {
    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT)
    startupComplete()

    while (isRunning) {
      val readyKeyNum = selector.select(1000)
      if (readyKeyNum > 0) {
        val readyKeys = selector.selectedKeys()
        val iter = readyKeys.iterator()

        var key: SelectionKey = null
        while (iter.hasNext) {
          key = iter.next()
          iter.remove()

        }

      }
    }

    shutdown()


  }


  private def accept(key: SelectionKey): Unit = {
    val ssc = key.channel().asInstanceOf[ServerSocketChannel]
    val sc = ssc.accept()
    ssc.configureBlocking(false)
    sc.register(selector, SelectionKey.OP_READ)
  }


  private def openSocket(): ServerSocketChannel = {
    val socketAddress =
      if (StringUtils.isBlank(host)) new InetSocketAddress(port) else new InetSocketAddress(host, port)
    val serverSocketChannel = ServerSocketChannel.open()
    serverSocketChannel.configureBlocking(false)
    try {
      serverSocketChannel.socket().bind(socketAddress)
      info("Awaiting socket connection on {}:{}", socketAddress.getHostName, port)
    } catch {
      case e: Exception => {
        throw new HexagonConnectException(
          "Socket Server failed to bind to %s:%d : %s".format(socketAddress.getHostName, port, e.getMessage), e)
      }
    }
    serverSocketChannel
  }
}





