package hexagon.client

import hexagon.protocol.Entity

class Client(val host: String, val port: Int) {


  def connect(): Unit = {

  }


  def serialize(): Entity = {
    ???
  }

  def put(topic: String, msg: String): Boolean = {
    ???
  }


  def query(topic: String, msg: String): Boolean = {
    ???
  }


}
