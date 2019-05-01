package hexagon.controller

import java.util.concurrent.atomic.AtomicBoolean

import hexagon.config.HexagonConfig
import hexagon.tools.Logging
import hexagon.utils.Locks._
import hexagon.zookeeper.{LeaderElectListener, ZkClient, ZkLeaderElector}
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.state.{ConnectionState, ConnectionStateListener}


object HexagonController extends Logging {

  val stateChangeLogger: StateChangeLogger = StateChangeLogger("state.change")

  case class StateChangeLogger(name: String) extends Logging


}


class HexagonController(val config: HexagonConfig,
                        val zkClient: ZkClient,
                        val brokerState: BrokerState) extends Logging {
  private val isRunning: AtomicBoolean = new AtomicBoolean(true)
  private val controllerContext = new ControllerContext(zkClient, config.zkSessionTimeout)
  private val controllerElector = new ZkLeaderElector(config.ControllerPath, controllerContext, new LeaderChangeListener, config.brokerId)

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


  def onResignation(): Unit = {

  }


  private def registerSessionExpirationListener(): Any = {
    ???
  }


  class SessionExpirationListener extends ConnectionStateListener with Logging {

    override def stateChanged(client: CuratorFramework, newState: ConnectionState): Unit = ???

  }


  class LeaderChangeListener extends LeaderElectListener with Logging {

    override def onBecomingLeader(): Unit = ???

    override def onResigningAsLeader(): Unit = ???
  }

}
