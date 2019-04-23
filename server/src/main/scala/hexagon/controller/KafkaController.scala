package hexagon.controller

import java.util.concurrent.atomic.AtomicBoolean

import hexagon.config.HexagonConfig
import hexagon.network.ZkClient
import hexagon.tools.Logging


object KafkaController extends Logging {


}


class KafkaController(val config: HexagonConfig,
                      val zkClient: ZkClient,
                      val brokerState: BrokerState) extends Logging {

  private val isRunning: AtomicBoolean = new AtomicBoolean(true)


}
