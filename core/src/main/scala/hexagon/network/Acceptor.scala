package hexagon.network

import java.net.InetSocketAddress
import java.nio.channels.{SelectionKey, ServerSocketChannel}

import hexagon.exceptions.HexagonConnectException
import hexagon.utils.StringUtils


private class Acceptor(val host: String,
                       val port: Int,
                       val sendBufferSize: Int,
                       val receiveBufferSize: Int,
                       val processor: Processor) extends AbstractServerThread {

  val serverSocketChannel: ServerSocketChannel = openSocket()

  override def run(): Unit = {
    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT)
    startupComplete()

    while (isRunning) {
      val readyKeyNum = selector.select(1000)
      if (readyKeyNum > 0) {
        val readyKeys = selector.selectedKeys()
        val itr = readyKeys.iterator()

        var key: SelectionKey = null
        while (itr.hasNext) {
          key = itr.next()
          itr.remove()
          if (key.isAcceptable)
            accept(key, processor)
          else
            throw new IllegalStateException("Not accept key in acceptor thread.")
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
        throw new HexagonConnectException(s"Socket Server failed to bind to ${socketAddress.getHostName}:${port} : ${e.getMessage}", e)
      }
    }
    serverSocketChannel
  }
}

