package hexagon.network

import java.net.InetSocketAddress
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel, SocketChannel}
import java.util.concurrent.{ConcurrentLinkedDeque, CountDownLatch}
import java.util.concurrent.atomic.AtomicBoolean

import hexagon.exceptions.HexagonConnectException
import hexagon.tools.{Logging, StringUtils}

private[hexagon] class SocketServer(private val port: Int,
                                    private val numProcessorThreads: Int) {


  private var selector: Selector = _
  private var serverSocketChannel: ServerSocketChannel = _

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

  protected val selector: Selector = Selector.open()
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

  def shutdownComplete(): Unit = shutdownLatch.countDown()


  def isRunning: Boolean = alive.get()
}


private class Acceptor(val host: String,
                       val port: Int,
                       val sendBufferSize: Int,
                       val receiveBufferSize: Int,
                       val processors: Array[Processor]) extends AbstractServerThread with Logging {

  val serverSocketChannel: ServerSocketChannel = openSocket()

  override def run(): Unit = {
    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT)
    startupComplete()

    var currentProcessor = 0
    while (isRunning) {
      val readyKeyNum = selector.select(1000)
      if (readyKeyNum > 0) {
        val readyKeys = selector.selectedKeys()
        val iter = readyKeys.iterator()

        var key: SelectionKey = null
        while (iter.hasNext) {
          key = iter.next()
          iter.remove()
          if (key.isAcceptable)
            accept(key, processors(currentProcessor))
          else
            throw new IllegalStateException("Not accept key in acceptor thread.")

          currentProcessor = (currentProcessor + 1) % processors.length
        }
      }
    }

    shutdownComplete()
  }


  private def accept(key: SelectionKey, processor: Processor): Unit = {
    val ssc = key.channel().asInstanceOf[ServerSocketChannel]
    ssc.socket().setReceiveBufferSize(receiveBufferSize)
    val sc = ssc.accept()
    ssc.configureBlocking(false)
    sc.register(selector, SelectionKey.OP_READ)
    sc.socket().setTcpNoDelay(true)
    sc.socket().setSendBufferSize(sendBufferSize)

    debug("Accepted connection from {} on {}. sendBufferSize [actual|requested]: [{}|{}] receiveBufferSize [actual|requested]: [{}|{}]",
      sc.socket.getInetAddress, sc.socket.getLocalSocketAddress,
      sc.socket.getSendBufferSize, sendBufferSize,
      sc.socket.getReceiveBufferSize, receiveBufferSize)

    processor.accept(sc)
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


private class Processor(val id: Int) extends AbstractServerThread {


  private val newConnection = new ConcurrentLinkedDeque[SocketChannel]()


  override def run(): Unit = {
    startupComplete()
    while (isRunning) {

    }
  }


  def read(key: SelectionKey): Unit = {
    val sc = key.channel().asInstanceOf[SocketChannel]
    key.attachment()
  }


  def accept(sc: SocketChannel): Unit = {
    newConnection.add(sc)
    selector.wakeup()
  }


  private def configNewConnections(): Unit = {
    while (!newConnection.isEmpty) {
      val sc = newConnection.poll()
      sc.register(selector, SelectionKey.OP_READ)
    }
  }


}




