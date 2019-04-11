package hexagon.handler

import hexagon.api.PutRequest
import hexagon.bloom.BloomFilterManager
import hexagon.network.{Receive, Send}
import hexagon.tools.SysTime

class PutRequestHandler(val bloomFilterManager: BloomFilterManager) extends Handler {


  override def handle(receive: Receive): Option[Send] = {
    val start = SysTime.mills
    val request = PutRequest.readFrom(receive.buffer)

    if (logger.isTraceEnabled())
      logger.trace(s"Put request $request")



    debug(s"Handle PutRequest used time: ${SysTime.elapsed(start)}")

    ???
  }


  private def handle(request: PutRequest): Boolean = {
    val bloomFilter = bloomFilterManager.getBloomFilter();
    bloomFilter.put(request.)
  }

}
