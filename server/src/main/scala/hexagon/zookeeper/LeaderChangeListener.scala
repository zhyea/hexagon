package hexagon.zookeeper

import org.apache.curator.framework.state.ConnectionState

trait LeaderChangeListener {

  def onBecomingLeader(client: ZkClient): Unit = ???

  def onResigningAsLeader(client: ZkClient, newState: ConnectionState): Unit = ???

}
