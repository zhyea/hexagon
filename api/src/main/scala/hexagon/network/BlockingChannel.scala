package hexagon.network

import java.net.InetSocketAddress
import java.nio.channels.{Channels, GatheringByteChannel, ReadableByteChannel, SocketChannel}

import hexagon.tools.Logging

class BlockingChannel(val host: String,
                      val port: Int,
                      val readBufferSize: Int,
                      val writeBufferSize: Int,
                      val readTimeoutMs: Int) extends Logging {

  private var connected = false
  private var channel: SocketChannel = null
  private var readChannel: ReadableByteChannel = null
  private var writeChannel: GatheringByteChannel = null
  private val lock = new Object()
  private val connectTimeoutMs = readTimeoutMs


  def connect(): Unit = lock synchronized {
    if (!connected) {
      try {
        channel = SocketChannel.open()
        if (readBufferSize > 0)
          channel.socket().setReceiveBufferSize(readBufferSize)
        if (writeBufferSize > 0)
          channel.socket().setSendBufferSize(writeBufferSize)

        channel.configureBlocking(true)
        channel.socket().setSoTimeout(readTimeoutMs)
        channel.socket().setKeepAlive(true)
        channel.socket().setTcpNoDelay(true)
        channel.socket().connect(new InetSocketAddress(host, port), connectTimeoutMs)

        writeChannel = channel
        readChannel = Channels.newChannel(channel.socket().getInputStream)
        connected = true

        val msg = "Created socket with SO_TIMEOUT = %d (requested %d), SO_RCVBUF = %d (requested %d), SO_SNDBUF = %d (requested %d), connectTimeoutMs = %d."
        debug(msg.format(channel.socket.getSoTimeout,
          readTimeoutMs,
          channel.socket.getReceiveBufferSize,
          readBufferSize,
          channel.socket.getSendBufferSize,
          writeBufferSize,
          connectTimeoutMs))
      } catch {
        case e: Throwable => disconnect()
      }
    }
  }


  def disconnect(): Unit = lock synchronized {
    if (null != channel) {
      swallow(channel.close())
      swallow(channel.socket().close())
      channel = null
      writeChannel = null
    }

    if (null != readChannel) {
      swallow(readChannel.close())
      readChannel = null
    }

    connected = false
  }


  def isConnected: Boolean = connected


  def send


}
