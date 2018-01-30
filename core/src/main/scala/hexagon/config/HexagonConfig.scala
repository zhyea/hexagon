package hexagon.config

import java.util.Properties

import hexagon.tools.PropKit

private[hexagon] class HexagonConfig(props: Properties) extends ZooKeeperConfig(props) {


  val port: Int = PropKit.getInt(props, "port", 9093)

  val host: String = PropKit.getString(props, "host", "127.0.0.1")

  val numNetworkThreads: Int = PropKit.getInt(props, "num.network.threads", Runtime.getRuntime.availableProcessors())

  val socketSendBuffer: Int = PropKit.getInt(props, "socket.send.buffer", 1024)

  val socketReceiveBuffer: Int = PropKit.getInt(props, "socket.receive.buffer", 1024)

  val maxMessageSize: Int = PropKit.getInt(props, "max.message.size", Int.MaxValue)


}
