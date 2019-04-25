package hexagon.zookeeper

import hexagon.controller.ControllerContext

class ZooKeeperLeaderElector(controllerContext: ControllerContext,
                             electionPath: String,
                             onBecomingLeader: () => Unit,
                             onResigningAsLeader: () => Unit,
                             brokerId: Int) extends LeaderElector {

  var leaderId = -1

  val index = electionPath.lastIndexOf("/")

  override def startup(): Unit = ???

  override def amILeader(): Boolean = leaderId == brokerId

  override def elect(): Boolean = ???

  override def close(): Unit = leaderId = -1


}
