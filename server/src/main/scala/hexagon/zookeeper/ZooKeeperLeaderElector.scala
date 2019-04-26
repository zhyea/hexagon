package hexagon.zookeeper

import hexagon.controller.ControllerContext

class ZooKeeperLeaderElector(controllerContext: ControllerContext,
                             zkClient: ZkClient,
                             electionPath: String,
                             listener: LeaderChangeListener,
                             brokerId: Int) extends LeaderElector {

  private var leaderId = -1

  private val index = electionPath.lastIndexOf("/")

  override def startup(): Unit = ???

  override def amILeader(): Boolean = leaderId == brokerId

  override def elect(): Boolean = ???

  override def close(): Unit = leaderId = -1


}
