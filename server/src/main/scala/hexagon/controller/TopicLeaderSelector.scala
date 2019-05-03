package hexagon.controller

import hexagon.cluster.LeaderAndIsr


trait TopicLeaderSelector {

  /**
    * 为指定topic选举leader
    *
    * @param topic               要选举leader的topic
    * @param currentLeaderAndIsr 从zookeeper中读取到的leader和isr信息
    * @return leader和isr请求：包括新选举出的leader和isr信息，以及要接收LeaderAndIsrRequest的replica集合
    */
  def selectLeader(topic: String, currentLeaderAndIsr: LeaderAndIsr): (LeaderAndIsr, Seq[Int])

}
