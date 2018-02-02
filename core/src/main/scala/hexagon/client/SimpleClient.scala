package hexagon.client

import java.net.{InetAddress, InetSocketAddress}
import java.nio.channels.SocketChannel

import hexagon.tools.Logging


class SimpleClient(private val host: String,
                   private val port: Int,
                   private val socketTimeout: Int,
                   private val receiveBufferSize: Int) extends Logging {


  private var channel: SocketChannel = _
  private val lock: Object = new Object


  def send(): Unit ={

  }


  def connect(): SocketChannel = {
    close()

    val address = new InetSocketAddress(host, port)
    val channel = SocketChannel.open()
    channel.configureBlocking(false)
    channel.socket().setReceiveBufferSize(receiveBufferSize)
    channel.socket().setSoTimeout(socketTimeout)
    channel.socket().setTcpNoDelay(true)
    channel.socket().setKeepAlive(true)

    channel.connect(address)

    channel
  }


  def close(): Unit = {
    lock.synchronized {
      if (null != channel)
        close(channel)
      channel = null
    }
  }


  private def close(channel: SocketChannel): Unit = {
    debug(s"Disconnecting from ${channel.getRemoteAddress}")
    swallow(channel.close())
    swallow(channel.socket().close())
  }

}
