package hexagon.network

import java.io.EOFException
import java.net.InetSocketAddress
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel, SocketChannel}
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ConcurrentLinkedQueue, CountDownLatch}

import hexagon.exception.InvalidRequestException
import hexagon.network.Handler.HandlerMapping
import hexagon.utils.{Logging, RequestKeys, Utils}

class SocketServer(val port: Int,
						 val numProcessorThreads: Int,
						 private val handlerFactory: HandlerMapping,
						 val sendBufferSize: Int,
						 val receiveBufferSize: Int,
						 val maxRequestSize: Int = Int.MaxValue) {

	private val processors: Array[Processor] = new Array[Processor](numProcessorThreads)
	private val acceptor: Acceptor = new Acceptor(port, processors, sendBufferSize, receiveBufferSize)

	def startup(): Unit = {
		for (i <- 0 until numProcessorThreads) {
			processors(i) = new Processor(handlerFactory, maxRequestSize)
			Utils.newThread("hexagon-processor-" + i, processors(i)).start()
		}
		Utils.newThread("hexagon-acceptor", acceptor).start()
		acceptor.awaitStartup()
	}

}


private abstract class AbstractServerThread extends Runnable with Logging {

	protected val selector: Selector = Selector.open()
	private val startupLatch = new CountDownLatch(1)
	private val shutdownLatch = new CountDownLatch(1)
	private val alive = new AtomicBoolean(false)


	def shutdown(): Unit = {
		alive.set(false)
		selector.wakeup()
		shutdownLatch.await()
	}

	def awaitStartup(): Unit = startupLatch.await()


	protected def startupComplete(): Unit = {
		alive.set(true)
		startupLatch.countDown()
	}

	protected def shutdownComplete(): Unit = shutdownLatch.countDown()


	protected def isRunning(): Boolean = alive.get()
}


private[network] class Acceptor(val port: Int,
										  private val processors: Array[Processor],
										  val sendBufferSize: Int,
										  val receiveBufferSize: Int) extends AbstractServerThread {

	override def run(): Unit = {
		val serverChannel = ServerSocketChannel.open()
		serverChannel.configureBlocking(false)
		serverChannel.socket().bind(new InetSocketAddress(port))
		serverChannel.register(selector, SelectionKey.OP_ACCEPT)

		info("Awaiting connection on port:{}.", port)
		startupComplete()

		var currentProcessor: Int = 0

		while (isRunning()) {
			val ready = selector.select(500)
			if (ready > 0) {
				val keys = selector.selectedKeys()
				val iter = keys.iterator()
				while (iter.hasNext && isRunning) {
					var key: SelectionKey = null
					try {
						key = iter.next()
						iter.remove()

						if (key.isAcceptable)
							accept(key, processors(currentProcessor))
						else
							throw new IllegalArgumentException("Unrecognized key state for acceptor thread.")

						currentProcessor = (currentProcessor + 1) % processors.length
					} catch {
						case e: Throwable => error("Error in acceptor.", e)
					}
				}
			}
		}

		debug("Closing server socket and selector.")
		swallow(error, serverChannel.close())
		swallow(error, selector.close())
		shutdownComplete()
	}


	def accept(key: SelectionKey, processor: Processor) {
		val serverSocketChannel = key.channel().asInstanceOf[ServerSocketChannel]
		serverSocketChannel.socket().setReceiveBufferSize(receiveBufferSize)

		val socketChannel = serverSocketChannel.accept()
		socketChannel.configureBlocking(true)
		socketChannel.socket().setTcpNoDelay(true)
		socketChannel.socket().setSendBufferSize(sendBufferSize)

		debug("sendBufferSize:[{}], receiveBufferSize:[{}]",
			socketChannel.socket().getSendBufferSize, socketChannel.socket().getReceiveBufferSize)

		processor.accept(socketChannel)
	}

}


private class Processor(val handlerMapping: HandlerMapping,
								val maxRequestSize: Int) extends AbstractServerThread {

	private val newConnections = new ConcurrentLinkedQueue[SocketChannel]()

	override def run(): Unit = {
		configureNewConnections()
		while (isRunning()) {
			val ready = selector.select(500)
			if (ready > 0) {
				val keys = selector.keys()
				val iter = keys.iterator()
				while (iter.hasNext && isRunning) {
					var key: SelectionKey = null
					try {
						key = iter.next()
						iter.remove()

						if (key.isReadable)
							read(key)
						else if (key.isWritable)
							write(key)
						else if (!key.isValid)
							close(key)
						else
							throw new IllegalStateException("Unrecognized key state for processor thread.")
					} catch {
						case e: EOFException => {
							info("Closing socket connection to {} due to EOF:{}", inetAddress(key), e.getMessage)
							close(key)
						}
						case e: InvalidRequestException => {
							info("Closing socket connection to {} due to invalid request:{}", inetAddress(key), e.getMessage)
							close(key)
						}
						case e: Throwable => {
							error("Closing socket for {} because of error.", inetAddress(key), e)
							close(key)
						}
					}
				}
			}
		}

		debug("Closing selector.")
		swallow(info, selector.close())
		shutdownComplete()
	}


	private def read(key: SelectionKey) {
		val socketChannel = channelFor(key)
		var request = key.attachment().asInstanceOf[Receive]

		if (null == key.attachment()) {
			request = new BoundedByteBufferReceive(maxRequestSize)
			key.attach(request)
		}
		val read = request.readFrom(socketChannel)

		trace("{} bytes read from {}", read, remoteAddress(key))

		if (read < 0) {
			close(key)
		} else if (request.complete) {
			val maybeResponse = handle(key, request)
			key.attach(null)
			if (maybeResponse.isDefined) {
				key.attach(maybeResponse.getOrElse(None))
				key.interestOps(SelectionKey.OP_WRITE)
			}
		} else {
			key.interestOps(SelectionKey.OP_READ)
			selector.wakeup()
		}
	}


	private def write(key: SelectionKey) {
		val response = key.attachment().asInstanceOf[Send]
		val written = response.writeTo(channelFor(key))

		trace("{} bytes write to {}.", written, remoteAddress(key))

		if (response.complete) {
			key.attach(null)
			key.interestOps(SelectionKey.OP_READ)
		} else {
			key.interestOps(SelectionKey.OP_WRITE)
			selector.wakeup()
		}
	}


	private def close(key: SelectionKey) {
		debug("Close connection from {}.", remoteAddress(key))
		val channel = channelFor(key)
		swallow(info, channel.socket().close())
		swallow(info, channel.close())
		key.attach(null)
		swallow(info, key.cancel())
	}


	def accept(channel: SocketChannel) {
		newConnections.add(channel)
		selector.wakeup()
	}


	private def handle(key: SelectionKey, request: Receive): Option[Send] = {
		val requestTypeId = request.buffer.getShort()

		requestTypeId match {
			case RequestKeys.Produce =>
				trace("Handling produce request from {}.", remoteAddress(key))
			case RequestKeys.Fetch =>
				trace("Handling fetch request from {}.", remoteAddress(key))
			case _ => throw new InvalidRequestException("No mapping found for handler id " + requestTypeId)
		}

		val handler = handlerMapping(requestTypeId, request)
		if (null == handler)
			throw new InvalidRequestException("No handler found for request")
		handler(request)
	}


	private def inetAddress(key: SelectionKey): String = channelFor(key).socket().getInetAddress.toString

	private def remoteAddress(key: SelectionKey): String = channelFor(key).socket().getRemoteSocketAddress.toString

	private def channelFor(key: SelectionKey): SocketChannel = key.channel().asInstanceOf[SocketChannel]


	private def configureNewConnections() {
		while (newConnections.size > 0) {
			val channel = newConnections.poll()
			debug("Listening to new connection from " + channel.socket.getRemoteSocketAddress)
			channel.register(selector, SelectionKey.OP_READ)
		}
	}
}