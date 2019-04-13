package hexagon.handler

import hexagon.api.RequestKeys
import hexagon.bloom.BloomFilterManager
import hexagon.network.{Receive, Send}

private[hexagon] class RequestHandlers(val bloomFilterManager: BloomFilterManager) extends Handler {

  private val putRequestHandler = new PutRequestHandler(bloomFilterManager)


  override def handle(receive: Receive): Option[Send] = {
    val requestId = receive.buffer.getShort

    try {

      requestId match {
        case RequestKeys.Put => putRequestHandler.handle(receive)
        case _ => throw new IllegalStateException(s"No mapping handler for id:$requestId")
      }

    } catch {
      case e: Throwable => error(s"Handling request with id:$requestId failed.", e); None
    }
  }


}
