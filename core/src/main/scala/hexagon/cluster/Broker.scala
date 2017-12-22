package hexagon.cluster

private[hexagon] object Broker {

	def createBroker(id: Int, brokerInfoString: String): Broker = {
		val brokerInfo: Array[String] = brokerInfoString.split(":")
		new Broker(id, brokerInfo(0), brokerInfo(1), brokerInfo(2).toInt)
	}

}

private[hexagon] class Broker(val id: Int,
										val creatorId: String,
										val hostName: String,
										val port: Int) {


	override def toString = s"id:$id, creatorId:$creatorId, host:$hostName, port:$port"

	def getZkString(): String = s"$creatorId:$hostName:$port"

	def canEqual(other: Any): Boolean = other.isInstanceOf[Broker]

	override def equals(other: Any): Boolean = other match {
		case that: Broker =>
			(that canEqual this) &&
				 id == that.id &&
				 hostName == that.hostName &&
				 port == that.port
		case _ => false
	}

	override def hashCode(): Int = {
		val state = Seq(id, hostName, port)
		state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
	}
}
