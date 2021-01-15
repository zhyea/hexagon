package hexagon.cluster

import hexagon.utils.NumberUtils._

case class Broker(id: Int, host: String, port: Int) {


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
