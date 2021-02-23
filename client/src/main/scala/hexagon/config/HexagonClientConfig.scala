package hexagon.config

import java.util.Properties

import hexagon.utils.PropKit._


class HexagonClientConfig(val props: Properties) {


	val host: String = getString(props, "host")

	val port: Int = getInt(props, "port")


	val maxMessageSize: Int = getInt(props, "max.message.size", 1000000)

	val bufferSize: Int = getInt(props, "buffer.size", 100 * 1024)

	val connectTimeoutMs: Int = getInt(props, "connect.timeout.ms", 5000)

	val socketTimeoutMs: Int = getInt(props, "socket.timeout.ms", 30000)

	val connectBackoffMs: Int = getInt(props, "connect.backoff.ms", 1000)

}
