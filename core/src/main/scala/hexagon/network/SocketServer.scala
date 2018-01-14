package hexagon.network

import java.net.InetSocketAddress
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel}

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



