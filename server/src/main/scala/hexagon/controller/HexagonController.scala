package hexagon.controller

import java.util.concurrent.atomic.AtomicBoolean

import hexagon.config.HexagonConfig
import hexagon.controller.HexagonController.StateChangeLogger
import hexagon.tools.Logging
import hexagon.utils.Locks._
import hexagon.zookeeper.ZkClient


object HexagonController extends Logging {


  case class StateChangeLogger(name: String) extends Logging

}


class HexagonController(val config: HexagonConfig,
                        val zkClient: ZkClient,
                        val brokerState: BrokerState) extends Logging {

  private val isRunning: AtomicBoolean = new AtomicBoolean(true)

  private val stateChangeLogger = new StateChangeLogger("state.change")

  private val controllerContext = new ControllerContext(zkClient)

  val clientId = s"id_${config.brokerId}—host_${config.host}-port_${config.port}"


  /**
    * 启动Controller
    */
  def startup(): Unit = {
    inLock(controllerContext.controllerLock) {
      info("Controller starting up")
      registerSessionExpirationListener()
      isRunning.set(true)

    }
  }


  private def registerSessionExpirationListener(): Any = {
    ???
  }
}
