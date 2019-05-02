package hexagon.client

import hexagon.api.PutRequest
import hexagon.network.BlockingChannel

class HexagonClient(override val host: String,
                    override val port: Int,
                    override val readBufferSize: Int = BlockingChannel.UseDefaultBufferSize,
                    override val writeBufferSize: Int = BlockingChannel.UseDefaultBufferSize,
                    override val socketTimeout: Int = 3600)
  extends BinaryClient(host, port, readBufferSize, writeBufferSize, socketTimeout) {


  def put(topic: String, msg: String): Boolean = {
    val response = put(PutRequest(topic, msg))
    response.result
  }


  def query(topic: String, msg: String): Boolean = {
    ???
  }


}
