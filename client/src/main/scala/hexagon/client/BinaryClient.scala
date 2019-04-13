package hexagon.client

import java.util.concurrent.atomic.AtomicBoolean

import hexagon.api.{PutRequest, PutResponse, RequestOrResponse}
import hexagon.network.{BlockingChannel, Receive}
import hexagon.tools.Logging
import hexagon.utils.NetUtils._

private[hexagon] class BinaryClient(val host: String,
                                    val port: Int,
                                    val readBufferSize: Int,
                                    val writeBufferSize: Int,
                                    val socketTimeout: Int) extends Logging {

  private val blockingChannel: BlockingChannel =
    new BlockingChannel(host, port, readBufferSize, writeBufferSize, socketTimeout)

  private val closed: AtomicBoolean = new AtomicBoolean(true)

  private val lock: Object = new Object()


  private def connect(): BlockingChannel = {
    lock synchronized {
      close()
      blockingChannel.connect()
      blockingChannel
    }
  }


  private def disconnect(): Unit = {
    debug(s"Disconnecting from ${formatAddress(host, port)}")
    lock synchronized {
      blockingChannel.disconnect()
    }
  }


  private def reconnect(): Unit = {
    disconnect()
    connect()
  }


  private def getOrMakeConnection(): Unit = {
    if (!isClosed && !blockingChannel.isConnected) {
      connect()
    }
  }


  def close(): Unit = {
    lock synchronized {
      disconnect()
      closed.set(true)
    }
  }

  def isClosed: Boolean = closed.get()


  private def sendRequest(request: RequestOrResponse): Receive = {
    lock synchronized {
      var response: Receive = null
      try {
        getOrMakeConnection()
        blockingChannel.send(request)
        response = blockingChannel.receive()
      } catch {
        case e: Throwable =>
          info(s"Reconnect due to socket error: ${e.toString}")
          try {
            reconnect()
            blockingChannel.send(request)
            response = blockingChannel.receive()
          } catch {
            case e: Throwable =>
              disconnect()
              throw e
          }
      }
      response
    }
  }


  def put(putRequest: PutRequest): PutResponse = {
    val receive = sendRequest(putRequest)
    PutResponse.readFrom(receive.buffer)
  }

}
