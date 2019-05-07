package hexagon.controller

import hexagon.api.{RequestOrResponse, TopicStateInfo}
import hexagon.cluster.LeaderAndIsr
import hexagon.config.HexagonConfig
import hexagon.tools.Logging

import scala.collection.{Set, mutable}

class ControllerBrokerRequest(controllerContext: ControllerContext,
                              config: HexagonConfig) extends Logging {

  /**
    * leader -> Map(topic->TopicStateInfo)
    */
  val leaderAndIsrRequestMap = new mutable.HashMap[Int, mutable.HashMap[String, TopicStateInfo]]
  val stopReplicaRequestMap = new mutable.HashMap[Int, Seq[StopReplicaRequestInfo]]
  val updateMetadataRequestMap = new mutable.HashMap[Int, mutable.HashMap[String, TopicStateInfo]]


  def addLeaderAndIsrRequestForBroker(brokerId: Int,
                                      topic: String,
                                      leaderAndIsr: LeaderAndIsr,
                                      controllerEpoch: Int,
                                      replicas: Seq[Int],
                                      callback: RequestOrResponse => Unit = null): Unit = {

    leaderAndIsrRequestMap.getOrElseUpdate(brokerId, new mutable.HashMap[String, TopicStateInfo]())
    leaderAndIsrRequestMap(brokerId).put(topic, TopicStateInfo(leaderAndIsr, controllerEpoch, replicas.toSet))

  }


  def addUpdateMetadataRequestForBrokers(brokerIds: Seq[Int],
                                         topics: collection.Set[String] = Set.empty[String],
                                         callback: RequestOrResponse => Unit = null) {
    def updateMetadataRequestMapFor(topic: String, beingDeleted: Boolean) {
      val leaderIsrAndControllerEpochOpt = controllerContext.topicLeadershipInfo.get(topic)
      leaderIsrAndControllerEpochOpt match {
        case Some(leaderIsrAndControllerEpoch) =>
          val replicas = controllerContext.topicReplicaAssignment(topic).toSet
          val partitionStateInfo = if (beingDeleted) {
            val leaderAndIsr = new LeaderAndIsr(LeaderAndIsr.LeaderDuringDelete, leaderIsrAndControllerEpoch.leaderAndIsr.isr)
            TopicStateInfo(leaderAndIsr, leaderIsrAndControllerEpoch.controllerEpoch, replicas)
          } else {
            TopicStateInfo(leaderIsrAndControllerEpoch.leaderAndIsr, leaderIsrAndControllerEpoch.controllerEpoch, replicas)
          }
          brokerIds.filter(b => b >= 0).foreach { brokerId =>
            updateMetadataRequestMap.getOrElseUpdate(brokerId, new mutable.HashMap[String, TopicStateInfo])
            updateMetadataRequestMap(brokerId).put(topic, partitionStateInfo)
          }
        case None =>
          info("Leader not yet assigned for partition %s. Skip sending UpdateMetadataRequest.".format(topic))
      }
    }

    val filteredPartitions = {
      val givenPartitions = if (topics.isEmpty)
        controllerContext.topicLeadershipInfo.keySet
      else
        topics
      if (controller.deleteTopicManager.partitionsToBeDeleted.isEmpty)
        givenPartitions
      else
        givenPartitions -- controller.deleteTopicManager.partitionsToBeDeleted
    }
    filteredPartitions.foreach(partition => updateMetadataRequestMapFor(partition, beingDeleted = false))
    controller.deleteTopicManager.partitionsToBeDeleted.foreach(partition => updateMetadataRequestMapFor(partition, beingDeleted = true))
  }


}
