package hexagon.server.nio

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel, SocketChannel}
import java.util.concurrent.atomic.AtomicBoolean

class NioServer(private val port: Int) {


  private var selector: Selector = _
  private var serverChannel: ServerSocketChannel = _

  try {
    selector = Selector.open
    serverChannel = ServerSocketChannel.open

    serverChannel.configureBlocking(false)
    serverChannel.socket().bind(new InetSocketAddress(port), 1024)

    serverChannel.register(selector, SelectionKey.OP_ACCEPT)


    println("Server started. Port is : " + port)
  } catch {
    case e: Exception => e.printStackTrace()
  }


  def start(): Unit = {

  }


  private def write(sc: SocketChannel): Unit = {
    val bytes: Array[Byte] = "response from server.".getBytes()
    val buffer = ByteBuffer.allocate(bytes.length)
    buffer.put(bytes)
    buffer.flip()
    sc.write(buffer)
  }
}


abstract class AbstractServerThread extends Runnable {


  private val alive: AtomicBoolean = new AtomicBoolean(false)

  val selector:Selector = Selector.open


  def isRunning = alive.get()

  def shutdown(): Unit = {
    if (null != selector) selector.close()
    alive.set(false)
  }

}


class Acceptor extends AbstractServerThread {

  override def run(): Unit = {
    while (isRunning) {
      selector.select(1000)
      val keys = selector.selectedKeys()
      val iter = keys.iterator()

      var key: SelectionKey = null
      while (iter.hasNext) {
        key = iter.next()
        iter.remove()
        if (key.isAcceptable)
          accept(key)
        else
          throw new RuntimeException("Illegal key state.")
      }
    }
    shutdown()
  }


  def accept(key: SelectionKey) = {
    val ssc = key.channel().asInstanceOf[ServerSocketChannel]
    val sc = ssc.accept()
    sc.configureBlocking(false)
    sc.register(selector, SelectionKey.OP_READ)
  }

}


class Processor extends AbstractServerThread {


  override def run(): Unit = {

  }


  private def read(key: SelectionKey): Unit = {
    key
  }
}


class Receive(id: Int, message: String)