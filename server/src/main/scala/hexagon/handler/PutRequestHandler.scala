package hexagon.handler

import hexagon.api.{PutRequest, PutResponse, RequestOrResponse}
import hexagon.bloom.BloomFilterManager
import hexagon.network.{BoundedByteBufferSend, Receive, Send}
import hexagon.tools.SysTime

class PutRequestHandler(val bloomFilterManager: BloomFilterManager) extends Handler {


  override def handle(receive: Receive): Option[Send] = {
    val start = SysTime.mills
    val request = PutRequest.readFrom(receive.buffer)

    if (logger.isTraceEnabled())
      logger.trace(s"Put request $request")

    val response = handle(request)
    debug(s"Handle PutRequest used time: ${SysTime.elapsed(start)}")

    Some(new BoundedByteBufferSend(response))
  }


  private def handle(request: PutRequest): RequestOrResponse = {
    val r = bloomFilterManager.getBloomFilter().put(request.msg)
    PutResponse(request.topic, r)
  }

}
