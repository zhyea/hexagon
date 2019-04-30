package hexagon.config

import java.util.Properties

import hexagon.utils.PropKit.{getBool, getInt, getString}

class ZooKeeperConfig(val props: Properties) {
  /**
    * Zookeeper Configs
    *
    */
  val zkNamespace = "hexagon"

  val ControllerPath: String = "/controller"

  val enableZooKeeper: Boolean = getBool(props, "enable.zookeeper")

  val zkConnect: String = getString(props, "zk.connect")

  val zkSessionTimeout: Int = getInt(props, "zk.session.timeout.ms", 6000)

  val zkConnectionTimeout: Int = getInt(props, "zk.connection.timeout.ms", zkSessionTimeout)

  val zkConnectRetryIntervalMs: Int = getInt(props, "zk.connect.retry.interval.ms", 6000)
}
