package hexagon.server

import hexagon.config.HexagonConfig
import hexagon.utils.{Logging, ZkStringSerializer}
import org.I0Itec.zkclient.{IZkStateListener, ZkClient}
import org.apache.zookeeper.Watcher.Event

class HexagonZooKeeper(val config: HexagonConfig) extends Logging {

  var zkClient: ZkClient = null
  val lock = new Object()

  def startup() {
    info("Connecting to ZK: " + config.zkHost)
    zkClient = new ZkClient(config.zkHost,
      config.zkSessionTimeoutMs,
      config.zkConnectionTimeoutMs,
      ZkStringSerializer)

    zkClient.subscribeStateChanges(new SessionExpireListener)
  }



  class SessionExpireListener extends IZkStateListener {

    override def handleSessionEstablishmentError(error: Throwable): Unit = ???

    override def handleStateChanged(state: Event.KeeperState): Unit = ???

    override def handleNewSession(): Unit = ???
  }

}