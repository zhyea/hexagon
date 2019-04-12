package hexagon.client

import hexagon.api.PutRequest
import hexagon.protocol.Entity

class Client(val host: String,
             val port: Int,
             val readBufferSize: Int = -1,
             val writeBufferSize: Int = -1,
             val socketTimeout: Int = 3600) {

  val client = new BinaryClient(host, port, readBufferSize, writeBufferSize, socketTimeout)

  def connect(): Unit = {

  }


  def serialize(): Entity = {
    ???
  }

  def put(topic: String, msg: String): Boolean = {
    val response = client.put(PutRequest(topic, msg))
    response.result
  }


  def query(topic: String, msg: String): Boolean = {
    ???
  }


}
