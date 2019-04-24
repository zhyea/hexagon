package hexagon.zookeeper

import hexagon.tools.Logging

trait LeaderElector extends Logging {

  def startup

  def amILeader


}
