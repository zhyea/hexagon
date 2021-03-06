package hexagon.server

import hexagon.config.HexagonServerConfig
import hexagon.tools.Logging

class HexagonServerBootable(config: HexagonServerConfig) extends Logging {

	private val server: HexagonServer = new HexagonServer(config)

	def startup(): Unit = {
		try {
			server.startup()
		} catch {
			case e: Exception => {
				error("Error during hexagon service's startup.", e)
				server.shutdown()
				System.exit(1)
			}
		}
	}


	def awaitShutdown(): Unit = {
		server.awaitShutdown()
	}


	def shutdown(): Unit = {
		try {
			server.shutdown()
		} catch {
			case e: Exception => {
				error("Error during hexagon service shutdown.", e)
				System.exit(1)
			}
		}
	}


}
