package hexagon.server.nio

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel, SocketChannel}
import java.util.concurrent.atomic.AtomicBoolean

class NioClient(private val host: String,
                private val port: Int) {

  private var selector: Selector = null
  private var socketChannel: SocketChannel = null
  private val isRunning = new AtomicBoolean(false)

  {
    selector = Selector.open()
    socketChannel = SocketChannel.open()
    socketChannel.configureBlocking(false)
    isRunning.set(true)

    println("Client started.")
  }

  def start(): Unit = {
    if (!socketChannel.connect(new InetSocketAddress(host, port))) {
      socketChannel.register(selector, SelectionKey.OP_CONNECT)
    }

    while (isRunning.get()) {
      selector.select(2000)
      val keys = selector.selectedKeys()
      val itr = keys.iterator()
      while (itr.hasNext) {
        val key = itr.next()
        itr.remove()
        handle(key)
      }
    }

    if (null != selector) selector.close()
  }


  private def handle(key: SelectionKey): Unit = {
    if (key.isValid) {
      if (key.isConnectable) {
        val sc = key.channel().asInstanceOf[SocketChannel]
        if (!sc.finishConnect()) {
          System.exit(1)
        }
      }
      if (key.isReadable) {
        read(key)
      }
    }
  }


  private def read(key: SelectionKey): Unit = {
    val sc = key.channel().asInstanceOf[SocketChannel]
    val buffer = ByteBuffer.allocate(1024)
    val read = sc.read(buffer)
    if (read > 0) {
      buffer.flip()
      val bytes = new Array[Byte](buffer.remaining())
      buffer.get(bytes)
      println(new String(bytes))
    } else if (read < 0) {
      key.cancel()
      sc.close()
    }
  }


  private def write(sc: SocketChannel, message: String): Unit = {
    val bytes: Array[Byte] = message.getBytes()
    val buffer = ByteBuffer.allocate(bytes.length)
    buffer.put(bytes)
    buffer.flip()
    sc.write(buffer)
  }

  def send(message: String): Unit = {
    socketChannel.register(selector, SelectionKey.OP_READ)
    write(socketChannel, message)
  }

}
