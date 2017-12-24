package hexagon.config

import java.util.Properties

import hexagon.utils.PropKit

class HexagonConfig(props: Properties) extends ZKConfig(props) {


  val port: Int = PropKit.getInt(props, "broker.port", 6667)

  val brokerIdsPath: String = "/brokers/ids"

  val brokerId: Int = PropKit.getInt(props, "broker.id")

  val hostName: String = PropKit.getString(props, "hostname")

}
