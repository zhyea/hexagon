package hexagon.network

import java.net.InetSocketAddress
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel}

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


private class Processor extends Runnable {

  override def run(): Unit = ???

}


private class Acceptor(val host: String,
                       val port: Int,
                       val sendBufferSize: Int,
                       val receiveBufferSize: Int) extends Runnable with Logging {
  val selector = Selector.open()
  val serverSocketChannel = openSocket()

  override def run(): Unit = ???


  def openSocket(): ServerSocketChannel = {
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


