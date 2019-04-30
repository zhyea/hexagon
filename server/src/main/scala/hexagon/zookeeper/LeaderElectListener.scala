package hexagon.zookeeper

trait LeaderElectListener {

  def onBecomingLeader(): Unit

  def onResigningAsLeader(): Unit

}
