package hexagon.zookeeper

import hexagon.controller.ControllerContext

class ZooKeeperLeaderElector(controllerContext: ControllerContext,
                             electionPath:String,
                             onBecomingLeader: ()=>Unit,
                             onResigningAsLeader: ()=>Unit,
                             brokerId:Int) extends LeaderElector {
  override def startup(): Unit = ???

  override def amILeader(): Boolean = ???

  override def elect(): Boolean = ???

  override def close(): Unit = ???
}
