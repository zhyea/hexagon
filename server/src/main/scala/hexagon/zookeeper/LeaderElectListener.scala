package hexagon.zookeeper

trait LeaderElectListener {

  def onBecomingLeader(client: ZkClient): Unit = ???

  def onResigningAsLeader(client: ZkClient): Unit = ???

}
