package hexagon.network

import java.io.EOFException
import java.nio.channels.{SelectionKey, SocketChannel}
import java.util.concurrent.ConcurrentLinkedQueue

import hexagon.exceptions.InvalidRequestException

private class Processor(val id: Int, val maxRequestSize: Int) extends AbstractServerThread {

	private val newConnection = new ConcurrentLinkedQueue[SocketChannel]()

	override def run(): Unit = {
		startupComplete()

		while (isRunning) {
			configNewConnections()

			if (selector.select(500) > 0) {

				val itr = selector.selectedKeys().iterator()
				while (itr.hasNext && isRunning) {
					val key = itr.next()

					val remoteHost = key.channel().asInstanceOf[SocketChannel].getRemoteAddress

					try {
						itr.remove()

						if (key.isReadable) {
							read(key)
						} else if (key.isWritable) {
							write(key)
						} else if (!key.isValid) {
							close(key)
						} else {
							throw new IllegalStateException("Unrecognized state for processor thread.")
						}
					} catch {
						case e: EOFException => error(s"Closing socket for $remoteHost.", e); close(key)
						case e: InvalidRequestException => error(s"Closing socket for $remoteHost due to invalid request.", e); close(key)
						case e: Throwable => error(s"Closing socket for $remoteHost because of error. ", e); close(key)
					}
				}
			}
		}
		debug("Closing selector..")
		swallowWarn(selector.close)
		shutdownComplete()
	}


	// 读取连接内容
	def read(key: SelectionKey): Unit = {

		var request: Receive = null
		if (null == key.attachment) {
			request = new BoundedByteBufferReceive(maxRequestSize)
			key.attach(request)
		} else {
			request = key.attachment().asInstanceOf[Receive]
		}

		val sc = key.channel().asInstanceOf[SocketChannel]
		val read = request.readFrom(sc)
		trace(s"$read bytes read from ${sc.getRemoteAddress}")

		if (read < 0) {
			close(key)
		} else if (request.isCompleted) {
			val response = handle(key, request)
			key.attach(null)
			if (response.isDefined) {
				key.attach(response.get)
				key.interestOps(SelectionKey.OP_WRITE)
			}
		} else {
			key.interestOps(SelectionKey.OP_READ)
			selector.wakeup()
		}
	}


	private def handle(key: SelectionKey, request: Receive): Option[Send] = {

		???
	}


	/**
	  * 将响应信息写入Channel
	  */
	def write(key: SelectionKey): Unit = {
		val sc = key.channel().asInstanceOf[SocketChannel]
		val response = key.attachment().asInstanceOf[Send]
		val written = response.writeTo(sc)
		trace(s"$written bytes written to ${sc.getRemoteAddress}")
		if (response.isCompleted) {
			key.attach(null)
			key.interestOps(SelectionKey.OP_READ)
		} else {
			key.interestOps(SelectionKey.OP_WRITE)
		}
	}


	/**
	  * 连接不可用时，将之移除
	  */
	def close(key: SelectionKey): Unit = {
		val channel = key.channel().asInstanceOf[SocketChannel]
		swallowWarn(channel.socket().close())
		swallowWarn(channel.close())
		key.attach(null)
		key.cancel()
	}


	/**
	  * 接收连接，并纳入处理队列
	  */
	def accept(sc: SocketChannel): Unit = {
		newConnection.add(sc)
		selector.wakeup()
	}


	/**
	  * 获取连接，并注册到当前Processor绑定的Selector上
	  */
	private def configNewConnections(): Unit = {
		while (!newConnection.isEmpty) {
			val sc = newConnection.poll()
			sc.register(selector, SelectionKey.OP_READ)
		}
	}
}

