package hexagon.client

import hexagon.protocol.Entity

class Client(val host: String, val port: Int) {


  def connect(): Unit = {

  }


  def serialize(): Entity = {
    ???
  }

  def put(record: Record[String]): Boolean = {
    ???
  }


  def query(record: Record[String]): Boolean = {
    ???
  }


}
