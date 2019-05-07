package hexagon.cluster

import hexagon.utils.JSON

object LeaderAndIsr {
  val initialLeaderEpoch: Int = 0
  val initialZKVersion: Int = 0
  val NoLeader: Int = -1
  val LeaderDuringDelete: Int = -2
}

case class LeaderAndIsr(var leader: Int,
                        var leaderEpoch: Int,
                        var isr: List[Int],
                        var zkVersion: Int) {

  def this(leader: Int,
           isr: List[Int]) = this(leader, LeaderAndIsr.initialLeaderEpoch, isr, LeaderAndIsr.initialZKVersion)

  override def toString: String = {
    JSON.toJson(Map("leader" -> leader, "leader_epoch" -> leaderEpoch, "isr" -> isr))
  }
}
