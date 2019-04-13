package hexagon.client

import hexagon.api.PutRequest

class HexagonClient(override val host: String,
                    override val port: Int,
                    override val readBufferSize: Int = -1,
                    override val writeBufferSize: Int = -1,
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
