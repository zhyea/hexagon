package hexagon.network

import java.io.EOFException
import java.net.InetSocketAddress
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel, SocketChannel}
import java.util.concurrent.{ConcurrentLinkedQueue, CountDownLatch}
import java.util.concurrent.atomic.AtomicBoolean

import hexagon.exceptions.{HexagonConnectException, InvalidRequestException}
import hexagon.tools.{Logging, StringUtils}

private[hexagon] class SocketServer(private val host: String,
                                    private val port: Int,
                                    private val numProcessorThreads: Int,
                                    private val sendBufferSize: Int,
                                    private val receiveBufferSize: Int) {
  private val processors = new Array[Processor](numProcessorThreads)
  private val acceptor = new Acceptor(host, port, sendBufferSize, receiveBufferSize, processors)

  def start(): Unit = {

  }


  def shutdown(): Unit = {
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


private class Processor(val id: Int,
                        val maxRequestSize: Int) extends AbstractServerThread {


  private val newConnection = new ConcurrentLinkedQueue[SocketChannel]()


  override def run(): Unit = {
    startupComplete()
    while (isRunning) {
      configNewConnections()
      val ready = selector.select(500)
      if (ready > 0) {
        val keys = selector.selectedKeys()
        val iter = keys.iterator()
        while (iter.hasNext && isRunning) {
          val key = iter.next()
          val remoteHost = key.channel().asInstanceOf[SocketChannel].getRemoteAddress
          try {
            iter.remove()

            if (key.isReadable) {
              read(key)
            } else if (key.isWritable) {
              write(key)
            } else if (!key.isValid) {
              close(key)
            } else {
              throw new IllegalStateException("Unrecognized state for processor thread.")
            }
          } catch {
            case e: EOFException => info(s"Closing socket for $remoteHost."); close(key)
            case e: InvalidRequestException => info(s"Closing socket for $remoteHost due to invalid request.", e); close(key)
            case e: Throwable => error(s"Closing socket for $remoteHost because of error. ", e); close(key)
          }
        }
      }
    }
    debug("Closing selector..")
    swallow(selector.close)
    shutdownComplete()
  }

  def handle(key: SelectionKey, request: Receive): Option[Send] = {
    ???
  }


  def read(key: SelectionKey): Unit = {
    val sc = key.channel().asInstanceOf[SocketChannel]
    var request = key.attachment().asInstanceOf[Receive]
    if (null == key.attachment) {
      request = new BoundedByteBufferReceive(maxRequestSize)
      key.attach(request)
    }
    val read = request.readFrom(sc)
    trace(s"$read bytes read from ${sc.getRemoteAddress}")
    if (read < 0) {
      close(key)
    } else if (request.complete) {
      val response = handle(key, request)
      key.attach(null)
      if (response.isDefined) {
        key.attach(response.getOrElse(None))
        key.interestOps(SelectionKey.OP_WRITE)
      }
    } else {
      key.interestOps(SelectionKey.OP_READ)
      selector.wakeup()
    }
  }


  def write(key: SelectionKey): Unit = {
    val sc = key.channel().asInstanceOf[SocketChannel]
    val response = key.attachment().asInstanceOf[Send]
    val written = response.writeTo(sc)
    trace(s"$written bytes written to ${sc.getRemoteAddress}")
    if (response.complete) {
      key.attach(null)
      key.interestOps(SelectionKey.OP_READ)
    } else {
      key.interestOps(SelectionKey.OP_WRITE)
    }
  }


  def close(key: SelectionKey): Unit = {
    val channel = key.channel().asInstanceOf[SocketChannel]
    swallow(channel.socket().close())
    swallow(channel.close())
    key.attach(null)
    key.cancel()
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




