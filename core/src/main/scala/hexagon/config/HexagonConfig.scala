package hexagon.config

import java.util.Properties

import hexagon.utils.PropKit._

private[hexagon] class HexagonConfig(props: Properties) extends ZooKeeperConfig(props) {


  val port: Int = getInt(props, "port", 8190)

  val host: String = getString(props, "host", "127.0.0.1")

  val numNetworkThreads: Int = getInt(props, "num.network.threads", Runtime.getRuntime.availableProcessors())

  val socketSendBuffer: Int = getInt(props, "socket.send.buffer", 1024)

  val socketReceiveBuffer: Int = getInt(props, "socket.receive.buffer", 1024)

  val maxMessageSize: Int = getInt(props, "max.message.size", Int.MaxValue)


}
