package hexagon.zookeeper

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener
import org.apache.curator.framework.state.ConnectionState

class LeaderChangeListener extends LeaderSelectorListener{

  override def takeLeadership(client: CuratorFramework): Unit = ???

  override def stateChanged(client: CuratorFramework, newState: ConnectionState): Unit = ???

}
