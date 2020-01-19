package hexagon.client

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import java.util.concurrent.atomic.AtomicBoolean

import hexagon.config.ClientConfig
import hexagon.network.BoundedByteBufferSend
import hexagon.tools.Logging
import hexagon.utils.SysTime


private[hexagon] class ClientThread(config: ClientConfig) extends Logging {


	private var channel: SocketChannel = _

	private val lock = new Object

	private val isRunning = new AtomicBoolean(true)


	private def send(send: BoundedByteBufferSend): Unit = {
		lock synchronized {
			makeConnection()

			try {
				send.writeComplete(channel)
			} catch {
				case e: IOException =>
					disconnect()
					throw e
				case e2 => throw e2
			}
		}
	}


	private def makeConnection(): Unit = {

		val start = SysTime.milli

		while (null == channel && isRunning.get()) {
			try {
				channel = SocketChannel.open()
				channel.socket().setSendBufferSize(config.bufferSize)
				channel.configureBlocking(false)
				channel.socket().setSoTimeout(config.socketTimeoutMs)
				channel.socket().setKeepAlive(true)
				channel.connect(new InetSocketAddress(config.host, config.port))

				info(s"Connected to ${config.host}:${config.port} for client.")
			} catch {
				case e: Exception =>
					disconnect()
					if (SysTime.elapsed(start) > config.connectTimeoutMs) {
						error(s"Client connection to ${config.host}:${config.port} timing out after ${config.connectTimeoutMs} ms", e)
						throw e;
					}
					error(s"Client attempt to ${config.host}:${config.port} failed, next attempt in ${config.connectBackoffMs} ms", e)
					SysTime.sleep(config.connectBackoffMs)
			}
		}
	}


	private def disconnect(): Unit = {
		try {
			if (null != channel) {
				info(s"Disconnecting from ${config.host}:${config.port}")
				swallowWarn(channel.close())
				swallowWarn(channel.socket().close())
				channel = null
			}
		} catch {
			case e: Exception => error(s"Error on disconnect from ${config.host}:${config.port} :", e)
		}
	}

}
