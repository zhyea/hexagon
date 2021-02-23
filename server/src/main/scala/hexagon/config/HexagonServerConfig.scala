package hexagon.config

import java.util.Properties
import hexagon.utils.PropKit._

/**
 * Hexagon配置管理类
 *
 * @param props 配置文件
 */
private[hexagon] class HexagonServerConfig(props: Properties) extends ZooKeeperConfig(props) {


	val host: String = getString(props, "host", "127.0.0.1")

	val port: Int = getInt(props, "port", 1022)

	val numNetworkThreads: Int = getInt(props, "num.network.threads", Runtime.getRuntime.availableProcessors())

	val socketSendBuffer: Int = getInt(props, "socket.send.buffer", 1024)

	val socketReceiveBuffer: Int = getInt(props, "socket.receive.buffer", 1024)

	val maxMessageSize: Int = getInt(props, "max.message.size", Int.MaxValue)


	/**
	 * 需要新增的配置项：
	 * * 保存的bloom的数量
	 * * 每个bloom的有效时间区间
	 */

}
