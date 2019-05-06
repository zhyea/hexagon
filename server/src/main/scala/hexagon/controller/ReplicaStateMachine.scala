package hexagon.controller

import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

import hexagon.cluster.TopicAndReplica
import hexagon.config.HexagonConfig
import hexagon.controller.HexagonController.StateChangeLogger
import hexagon.tools.Logging
import hexagon.utils.Locks
import hexagon.zookeeper.{NodeListener, PathChildrenListener, ZkClient}
import org.apache.curator.framework.recipes.cache.{ChildData, PathChildrenCache}

import scala.collection.mutable

class ReplicaStateMachine(controllerContext: ControllerContext,
                          config: HexagonConfig,
                          stateChangeLogger: StateChangeLogger) extends Logging {


  private val controllerId = config.brokerId
  private val zkClient = controllerContext.zkClient
  private val replicaState: mutable.Map[TopicAndReplica, ReplicaState] = mutable.Map.empty
  private val brokerChangeListener = new BrokerChangeListener()
  private val brokerRequestBatch = new ControllerBrokerRequestBatch(controller)
  private val hasStarted = new AtomicBoolean(false)

  private val brokerCache: PathChildrenCache = zkClient.createPathChildrenCache(config.BrokerIdsPath, true)


  class BrokerChangeListener() extends PathChildrenListener(brokerCache) {

    override def onDataChange(childData: ChildData): Unit = {

      Locks.inLock(controllerContext.controllerLock) {
        if (null != childData && hasStarted.get()) {
          try {
            val path = childData.getPath
            val data = new String(childData.getData, StandardCharsets.UTF_8)


          } catch {
            case e: Throwable => error("Error while handling broker changes", e)
          }
        }
      }
      //end on data change
    }


    private def getBrokerId(path: String): Int = path.substring(path.lastIndexOf('/')).toInt


    //end broker change listener
  }

}


sealed trait ReplicaState {
  def state: Byte
}

case object NewReplica extends ReplicaState {
  val state: Byte = 1
}

case object OnlineReplica extends ReplicaState {
  val state: Byte = 2
}

case object OfflineReplica extends ReplicaState {
  val state: Byte = 3
}

case object ReplicaDeletionStarted extends ReplicaState {
  val state: Byte = 4
}

case object ReplicaDeletionSuccessful extends ReplicaState {
  val state: Byte = 5
}

case object ReplicaDeletionIneligible extends ReplicaState {
  val state: Byte = 6
}

case object NonExistentReplica extends ReplicaState {
  val state: Byte = 7
}
