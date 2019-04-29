package hexagon.zookeeper

trait ZkStateListener {

  def onDisconnect()

  def onConnect()


}
