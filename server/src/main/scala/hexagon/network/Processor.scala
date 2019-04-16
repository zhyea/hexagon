package hexagon.network

import java.io.EOFException
import java.nio.channels.{SelectionKey, SocketChannel}
import java.util.concurrent.ConcurrentLinkedQueue

import hexagon.exceptions.InvalidRequestException
import hexagon.handler.RequestHandlers
import hexagon.utils.NetUtils._

private class Processor(val handlers: RequestHandlers, val maxRequestSize: Int) extends AbstractServerThread {

  private val newConnection = new ConcurrentLinkedQueue[SocketChannel]()

  override def run(): Unit = {
    startupComplete()
    while (isRunning) {
      configNewConnections()
      val ready = selector.select(500)
      if (ready > 0) {
        val keys = selector.selectedKeys()
        val itr = keys.iterator()
        while (itr.hasNext && isRunning) {
          val key = itr.next()
          val remoteHost = channelOf(key).getRemoteAddress
          try {
            itr.remove()

            checkKey(key)

            if (key.isReadable) {
              read(key)
            } else if (key.isWritable) {
              write(key)
            } else if (!key.isValid) {
              close(key)
            } else {
              //throw new IllegalStateException("Unrecognized state for processor thread.")
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
    swallow(selector.close())
    shutdownComplete()
  }

  def read(key: SelectionKey): Unit = {
    val sc = channelOf(key)
    var receive = key.attachment().asInstanceOf[Receive]
    if (null == key.attachment) {
      receive = new BoundedByteBufferReceive(maxRequestSize)
      key.attach(receive)
    }

    val read = receive.readFrom(sc)
    trace(s"$read bytes read from ${sc.getRemoteAddress}")

    if (read < 0) {
      close(key)
    } else if (receive.isCompleted) {
      val response = handle(key, receive)
      key.attach(null)
      if (response.isDefined) {
        key.attach(response.getOrElse(None))
        key.interestOps(SelectionKey.OP_WRITE)
      }
    } else {
      key.interestOps(SelectionKey.OP_READ)
      selector.wakeup()
    }
  }


  private def handle(key: SelectionKey, receive: Receive): Option[Send] = {
    handlers.handle(receive)
  }


  def write(key: SelectionKey): Unit = {
    val sc = channelOf(key)
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


  def close(key: SelectionKey): Unit = {
    val channel = channelOf(key)
    swallow(channel.socket().close())
    swallow(channel.close())
    key.attach(null)
    key.cancel()
  }


  def accept(sc: SocketChannel): Unit = {
    newConnection.add(sc)
    selector.wakeup()
  }


  private def configNewConnections(): Unit = {
    while (!newConnection.isEmpty) {
      val sc = newConnection.poll()
      sc.register(selector, SelectionKey.OP_READ)
    }
  }


  private def checkKey(key: SelectionKey): Unit = {
    if (key.isReadable) {
      println("-----------this key is readable")
    } else if (key.isWritable) {
      println("-----------this key is writable")
    } else if (!key.isValid) {
      println("-----------this key is not valid")
    } else {
      println("-----------this key is others")
    }
  }
}

