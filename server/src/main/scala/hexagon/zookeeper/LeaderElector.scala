package hexagon.zookeeper

import hexagon.tools.Logging

trait LeaderElector extends Logging {

  def startup(): Unit

  def amILeader(): Boolean

  def elect(): Boolean

  def close(): Unit

}
