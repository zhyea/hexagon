package hexagon.network

import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel

class SocketServer(private val port: Int) {

  private val serverSocketChannel: ServerSocketChannel = ServerSocketChannel.open()

  def start(): Unit = {
    serverSocketChannel.socket().bind(new InetSocketAddress(port))
    while (true) {
      val socketChannel = serverSocketChannel.accept()
      if (null != socketChannel) {
        // do something
      }
    }
  }


  def shutdown(): Unit = {
    serverSocketChannel.close()
  }

}
