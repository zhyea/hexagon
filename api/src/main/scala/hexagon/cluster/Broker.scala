package hexagon.cluster

import java.nio.ByteBuffer

import hexagon.exceptions.{BrokerNotAvailableException, HexagonException}
import hexagon.utils.IOUtils._
import hexagon.utils.JSON
import hexagon.utils.NetUtils._
import hexagon.utils.NumUtils._

object Broker {


  def createBroker(id: Int, brokerInfoString: String): Broker = {
    if (brokerInfoString == null)
      throw new BrokerNotAvailableException(s"Broker id $id does not exist")
    try {
      JSON.parseFull(brokerInfoString) match {
        case Some(m) =>
          val brokerInfo = m.asInstanceOf[Map[String, Any]]
          val host = brokerInfo("host").asInstanceOf[String]
          val port = brokerInfo("port").asInstanceOf[Int]
          new Broker(id, host, port)
        case None =>
          throw new BrokerNotAvailableException(s"Broker id $id does not exist")
      }
    } catch {
      case t: Throwable => throw new HexagonException(s"Failed to parse the broker info from zookeeper: $brokerInfoString", t)
    }
  }


  def readFrom(buffer: ByteBuffer): Broker = {
    val id = buffer.getInt
    val host = readShortString(buffer)
    val port = buffer.getInt
    new Broker(id, host, port)
  }
}


case class Broker(id: Int, host: String, port: Int) {

  def connectionString: String = formatAddress(host, port)

  def writeTo(buffer: ByteBuffer) {
    buffer.putInt(id)
    writeShortString(buffer, host)
    buffer.putInt(port)
  }

  def sizeInBytes: Int =
    shortStringLength(host) /* host name */ + Integer.BYTES /* port */ + Integer.BYTES /* broker id*/


  override def toString: String = s"id:$id,host:$host,post:$port"

  override def equals(obj: Any): Boolean = {
    obj match {
      case null => false
      case b: Broker => id == b.id && host == b.host && port == b.port
      case _ => false
    }
  }

  override def hashCode(): Int = hashcode(id, host, port)

}
