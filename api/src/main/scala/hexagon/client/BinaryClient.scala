package hexagon.client

import hexagon.api.PutRequest
import hexagon.network.BlockingChannel
import hexagon.protocol.Entity
import hexagon.serializer.Encoder

class BinaryClient[T](val host: String,
                      val port: Int,
                      val encoder: Encoder[T]) {

  private val blockingChannel: BlockingChannel = new BlockingChannel(host, port)


  private val lock: Object = new Object()


  def connect(): Unit = {
    lock synchronized {
      blockingChannel.connect()
    }
  }


  def disconnect(): Unit = {
    lock synchronized {
      blockingChannel.disconnect()
    }
  }


  def put(record: Record[Entity]): Boolean = {
    val req = PutRequest(record.topic, record.value)
    blockingChannel.send(req)
    ???
  }

}
