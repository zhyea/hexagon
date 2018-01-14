package hexagon.config

import java.util.Properties

import hexagon.tools.PropKit

private[hexagon] class HexagonConfig(props: Properties)
  extends ZooKeeperConfig(props) {


  val port: Int = PropKit.getInt(props, "port", 9093)


}
