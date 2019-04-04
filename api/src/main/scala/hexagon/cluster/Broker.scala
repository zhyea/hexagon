package hexagon.cluster

import java.nio.ByteBuffer
import hexagon.utils.IOUtils._
import hexagon.utils.NetUtils._
import hexagon.utils.NumUtils._

case class Broker(id: Int, host: String, port: Int){

  override def toString: String = "id:" + id + ",host:" + host + ",port:" + port

  def connectionString: String = formatAddress(host, port)

  def writeTo(buffer: ByteBuffer) {
    buffer.putInt(id)
    writeShortString(buffer, host)
    buffer.putInt(port)
  }

  def sizeInBytes: Int = shortStringLength(host) /* host name */ + 4 /* port */ + 4 /* broker id*/

  override def equals(obj: Any): Boolean = {
    obj match {
      case null => false
      case n: Broker => id == n.id && host == n.host && port == n.port
      case _ => false
    }
  }

  override def hashCode(): Int = hashcode(id, host, port)
}
