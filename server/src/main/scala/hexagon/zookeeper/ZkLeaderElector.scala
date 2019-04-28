package hexagon.zookeeper

import hexagon.controller.{Controller, ControllerContext}
import hexagon.tools.Logging
import hexagon.utils.JSON
import hexagon.utils.Locks._
import org.apache.zookeeper.KeeperException.NodeExistsException

sealed trait State


class ZkLeaderElector(leaderPath: String,
                      controllerContext: ControllerContext,
                      listener: LeaderElectListener,
                      brokerId: Int) extends LeaderElector {


  private var leaderId = -1
  private val zkClient = controllerContext.zkClient

  zkClient.createParentPathIfNeeded(leaderPath)


  override def startup(): Unit = {
    inLock(controllerContext.controllerLock) {
      zkClient
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

  override def close(): Unit = ???


  override def resign(): Boolean = ???


  private def getControllerId: Int = {
    zkClient.readDataMaybeNull(leaderPath) match {
      case Some(json) => Controller.parse(json).brokerId
      case None => -1
    }
  }


  class LeaderChangeListener extends Logging {


    def onDataChange(): Unit = {
      inLock(controllerContext.controllerLock){
        leaderId = Controller.parse()
      }



      ???
    }


    def onDataDelete(): Unit = {
      ???
    }

  }

}
