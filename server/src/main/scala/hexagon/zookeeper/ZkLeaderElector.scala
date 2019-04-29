package hexagon.zookeeper

import java.nio.charset.StandardCharsets

import hexagon.controller.{Controller, ControllerContext}
import hexagon.tools.Logging
import hexagon.utils.JSON
import hexagon.utils.Locks._
import org.apache.curator.framework.recipes.cache.{ChildData, NodeCacheListener}
import org.apache.zookeeper.KeeperException.NodeExistsException


class ZkLeaderElector(leaderPath: String,
                      controllerContext: ControllerContext,
                      listener: LeaderElectListener,
                      brokerId: Int) extends LeaderElector {


  private var leaderId = -1
  private val zkClient = controllerContext.zkClient
  private val cache = zkClient.createNodeCache(leaderPath)

  zkClient.createParentPathIfNeeded(leaderPath)


  override def startup(): Unit = {
    inLock(controllerContext.controllerLock) {
      cache.getListenable.addListener(new LeaderChangeListener())
      cache.start(true)
      elect()
    }
  }

  override def amILeader(): Boolean = getControllerId == brokerId

  override def elect(): Boolean = {
    leaderId = getControllerId
    if (leaderId != -1) {
      debug(s"Broker $leaderId has been selected as leader, so stopping the election process")
      return amILeader()
    }

    val electJson = JSON.toJson(Controller(brokerId))

    try {
      zkClient.createEphemeralPathExpectConflictHandleZKBug(leaderPath, electJson, brokerId,
        (controllerString: String, leaderId: Any) => Controller.parse(controllerString).brokerId == leaderId.asInstanceOf[Int],
        controllerContext.zkSessionTimeout
      )
      info(s"$brokerId successfully elected as leader")
      leaderId = brokerId
      listener.onBecomingLeader(zkClient)
    } catch {
      case e: NodeExistsException =>
        leaderId = getControllerId
        if (leaderId != -1)
          debug(s"Broker $leaderId was elected as leader instead of broker $brokerId")
        else
          warn("A leader has been elected but just resigned, this will result in another round of election")

      case e1: Throwable =>
        error(s"Error while electing or becoming leader on broker $brokerId", e1)
        resign()
    }

    amILeader()
  }

  override def close(): Unit = {
    leaderId = -1
    cache.close()
  }


  override def resign(): Boolean = {
    leaderId = -1
    zkClient.deletePath(leaderPath)
  }


  private def getControllerId: Int = {
    zkClient.readDataMaybeNull(leaderPath) match {
      case Some(json) => Controller.parse(json).brokerId
      case None => -1
    }
  }


  class LeaderChangeListener extends NodeCacheListener with Logging {

    override def nodeChanged(): Unit = {
      val data = cache.getCurrentData
      if (null == data) onNodeDelete()
      else onDataChange(data)
    }

    private def onDataChange(data: ChildData): Unit = {
      inLock(controllerContext.controllerLock) {
        leaderId = readLeaderId(data)
        info(s"New leader is $leaderId")
      }
    }


    private def onNodeDelete(): Unit = {
      inLock(controllerContext.controllerLock) {
        debug(s"$brokerId leader change listener fired for path $leaderPath to handle data deleted: trying to elect as a leader")
        if (amILeader()) {
          listener.onResigningAsLeader(zkClient)
        }
        elect()
      }
    }


    def readLeaderId(data: ChildData): Int = {
      val bytes = data.getData
      val controllerInfo = new String(bytes, StandardCharsets.UTF_8)
      Controller.parse(controllerInfo).brokerId
    }
  }

}
