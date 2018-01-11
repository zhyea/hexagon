package hexagon.server.nio

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel, SocketChannel}

class NioClient(private val host: String,
                private val port: Int) {

  private var socketChannel: SocketChannel = null

  {
    socketChannel = SocketChannel.open()
    socketChannel.configureBlocking(false)
    socketChannel.connect(new InetSocketAddress(host, port))
  }

  def start(): Unit = {
    val bytes: Array[Byte] = "message from client.".getBytes()
    val buffer = ByteBuffer.allocate(bytes.length)
    buffer.put(bytes)
    buffer.flip()
    socketChannel.write(buffer)
  }

}
