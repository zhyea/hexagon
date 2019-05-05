package hexagon.controller

import hexagon.cluster.LeaderAndIsr
import hexagon.config.HexagonConfig
import hexagon.exceptions.{LeaderElectionNotNeededException, StateChangeFailedException}
import hexagon.tools.Logging


trait TopicLeaderSelector extends Logging {

  /**
    * 为指定topic选举leader
    *
    * @param topic               要选举leader的topic
    * @param currentLeaderAndIsr 从zookeeper中读取到的leader和isr信息
    * @return leader和isr请求：包括新选举出的leader和isr信息，以及要接收LeaderAndIsrRequest的replica集合
    */
  def selectLeader(topic: String, currentLeaderAndIsr: LeaderAndIsr): (LeaderAndIsr, Seq[Int])

}


/**
  * Select the new leader, new isr and receiving replicas (for the LeaderAndIsrRequest):
  * 1. If at least one broker from the isr is alive, it picks a broker from the live isr as the new leader and the live
  * isr as the new isr.
  * 2. Else, if unclean leader election for the topic is disabled, it throws a NoReplicaOnlineException.
  * 3. Else, it picks some alive broker from the assigned replica list as the new leader and the new isr.
  * 4. If no broker in the assigned replica list is alive, it throws a NoReplicaOnlineException
  * Replicas to receive LeaderAndIsr request = live assigned replicas
  * Once the leader is successfully registered in zookeeper, it updates the allLeaders cache
  */
class OfflineTopicLeaderSelector(controllerContext: ControllerContext, config: HexagonConfig)
  extends TopicLeaderSelector {

  def selectLeader(topic: TopicAndPartition, currentLeaderAndIsr: LeaderAndIsr): (LeaderAndIsr, Seq[Int]) = {
    controllerContext.topicReplicaAssignment.get(topic) match {
      case Some(assignedReplicas) =>
        val liveAssignedReplicas = assignedReplicas.filter(r => controllerContext.liveBrokerIds.contains(r))
        val liveBrokersInIsr = currentLeaderAndIsr.isr.filter(r => controllerContext.liveBrokerIds.contains(r))
        val currentLeaderEpoch = currentLeaderAndIsr.leaderEpoch
        val currentLeaderIsrZkPathVersion = currentLeaderAndIsr.zkVersion
        val newLeaderAndIsr = liveBrokersInIsr.isEmpty match {
          case true =>
            // Prior to electing an unclean (i.e. non-ISR) leader, ensure that doing so is not disallowed by the configuration
            // for unclean leader election.
            if (!LogConfig.fromProps(config.props.props, AdminUtils.fetchTopicConfig(controllerContext.zkClient,
              topic.topic)).uncleanLeaderElectionEnable) {
              throw new NoReplicaOnlineException(("No broker in ISR for partition " +
                "%s is alive. Live brokers are: [%s],".format(topic, controllerContext.liveBrokerIds)) +
                " ISR brokers are: [%s]".format(currentLeaderAndIsr.isr.mkString(",")))
            }

            debug("No broker in ISR is alive for %s. Pick the leader from the alive assigned replicas: %s"
              .format(topic, liveAssignedReplicas.mkString(",")))
            liveAssignedReplicas.isEmpty match {
              case true =>
                throw new NoReplicaOnlineException(("No replica for partition " +
                  "%s is alive. Live brokers are: [%s],".format(topic, controllerContext.liveBrokerIds)) +
                  " Assigned replicas are: [%s]".format(assignedReplicas))
              case false =>
                ControllerStats.uncleanLeaderElectionRate.mark()
                val newLeader = liveAssignedReplicas.head
                warn("No broker in ISR is alive for %s. Elect leader %d from live brokers %s. There's potential data loss."
                  .format(topic, newLeader, liveAssignedReplicas.mkString(",")))
                new LeaderAndIsr(newLeader, currentLeaderEpoch + 1, List(newLeader), currentLeaderIsrZkPathVersion + 1)
            }
          case false =>
            val liveReplicasInIsr = liveAssignedReplicas.filter(r => liveBrokersInIsr.contains(r))
            val newLeader = liveReplicasInIsr.head
            debug("Some broker in ISR is alive for %s. Select %d from ISR %s to be the leader."
              .format(topic, newLeader, liveBrokersInIsr.mkString(",")))
            new LeaderAndIsr(newLeader, currentLeaderEpoch + 1, liveBrokersInIsr.toList, currentLeaderIsrZkPathVersion + 1)
        }
        info("Selected new leader and ISR %s for offline partition %s".format(newLeaderAndIsr.toString(), topic))
        (newLeaderAndIsr, liveAssignedReplicas)
      case None =>
        throw new NoReplicaOnlineException("Partition %s doesn't have replicas assigned to it".format(topic))
    }
  }
}

/**
  * New leader = a live in-sync reassigned replica
  * New isr = current isr
  * Replicas to receive LeaderAndIsr request = reassigned replicas
  */
class ReassignedTopicLeaderSelector(controllerContext: ControllerContext) extends TopicLeaderSelector with Logging {

  /**
    * The reassigned replicas are already in the ISR when selectLeader is called.
    */
  def selectLeader(topic: String, currentLeaderAndIsr: LeaderAndIsr): (LeaderAndIsr, Seq[Int]) = {
    val reassignedInSyncReplicas = controllerContext.topicBeingReassigned(topic).newReplicas
    val currentLeaderEpoch = currentLeaderAndIsr.leaderEpoch
    val currentLeaderIsrZkPathVersion = currentLeaderAndIsr.zkVersion
    val aliveReassignedInSyncReplicas = reassignedInSyncReplicas.filter(r => controllerContext.liveBrokerIds.contains(r) &&
      currentLeaderAndIsr.isr.contains(r))
    val newLeaderOpt = aliveReassignedInSyncReplicas.headOption
    newLeaderOpt match {
      case Some(newLeader) => (new LeaderAndIsr(newLeader, currentLeaderEpoch + 1, currentLeaderAndIsr.isr,
        currentLeaderIsrZkPathVersion + 1), reassignedInSyncReplicas)
      case None =>
        reassignedInSyncReplicas.size match {
          case 0 =>
            throw new NoReplicaOnlineException("List of reassigned replicas for partition " +
              " %s is empty. Current leader and ISR: [%s]".format(topic, currentLeaderAndIsr))
          case _ =>
            throw new NoReplicaOnlineException("None of the reassigned replicas for partition " +
              "%s are in-sync with the leader. Current leader and ISR: [%s]".format(topic, currentLeaderAndIsr))
        }
    }
  }
}


/**
  * 新的leader为第一个分配的replica
  * 新的isr为当前isr
  * 要接收LeaderAndIsrRequest的replica为所有已分配的replica
  */
class PreferredReplicaTopicLeaderSelector(controllerContext: ControllerContext) extends TopicLeaderSelector {

  def selectLeader(topic: String, currentLeaderAndIsr: LeaderAndIsr): (LeaderAndIsr, Seq[Int]) = {
    val assignedReplicas = controllerContext.topicReplicaAssignment(topic)
    val preferredReplica = assignedReplicas.head
    // 检查preferred replica是否是当前leader
    val currentLeader = controllerContext.topicLeadershipInfo(topic).leaderAndIsr.leader
    if (currentLeader == preferredReplica) {
      throw new LeaderElectionNotNeededException(s"Preferred replica $preferredReplica is already the current leader for topic $topic")
    } else {
      info(s"Current leader $currentLeader for topic $topic is not the preferred replica. Triggering preferred replica leader election")
      // 再次确认preferred replica不是当前leader，并且在isr列表中，且处于alive状态
      if (controllerContext.liveBrokerIds.contains(preferredReplica) && currentLeaderAndIsr.isr.contains(preferredReplica)) {
        (new LeaderAndIsr(preferredReplica, currentLeaderAndIsr.leaderEpoch + 1, currentLeaderAndIsr.isr, currentLeaderAndIsr.zkVersion + 1), assignedReplicas)
      } else {
        throw new StateChangeFailedException(s"Preferred replica $preferredReplica for topic $topic is either not alive or not in the isr. Current leader and ISR: [$currentLeaderAndIsr]")
      }
    }
  }
}


/**
  * 新的leader为isr中未shutdown的replica中的第一个
  * 新的isr为当前isr中未shutdown的replica
  * 要接收LeaderAndIsrRequest的replica为所有未shutdown的replica
  */
class ControlledShutdownLeaderSelector(controllerContext: ControllerContext) extends TopicLeaderSelector {

  def selectLeader(topic: String, currentLeaderAndIsr: LeaderAndIsr): (LeaderAndIsr, Seq[Int]) = {
    val currentLeaderEpoch = currentLeaderAndIsr.leaderEpoch
    val currentLeaderIsrZkPathVersion = currentLeaderAndIsr.zkVersion

    val currentLeader = currentLeaderAndIsr.leader

    val assignedReplicas = controllerContext.topicReplicaAssignment(topic)
    val liveOrShuttingDownBrokerIds = controllerContext.liveOrShuttingDownBrokerIds
    val liveAssignedReplicas = assignedReplicas.filter(r => liveOrShuttingDownBrokerIds.contains(r))

    val newIsr = currentLeaderAndIsr.isr.filter(brokerId => !controllerContext.shuttingDownBrokerIds.contains(brokerId))
    val newLeaderOpt = newIsr.headOption
    newLeaderOpt match {
      case Some(newLeader) =>
        debug(s"Topic $topic : current leader = $currentLeader, new leader = $newLeader")
        (LeaderAndIsr(newLeader, currentLeaderEpoch + 1, newIsr, currentLeaderIsrZkPathVersion + 1), liveAssignedReplicas)
      case None =>
        throw new StateChangeFailedException(s"No other replicas in ISR ${currentLeaderAndIsr.isr.mkString(",")} for $topic besides shutting down brokers ${controllerContext.shuttingDownBrokerIds.mkString(",")}")
    }
  }
}


/**
  * 什么也没做，只是返回当前的leader和isr
  */
class NoOpLeaderSelector(controllerContext: ControllerContext) extends TopicLeaderSelector {

  override def selectLeader(topic: String, currentLeaderAndIsr: LeaderAndIsr): (LeaderAndIsr, Seq[Int]) = {
    warn("I should never have been asked to perform leader election, returning the current LeaderAndIsr and replica assignment.")
    (currentLeaderAndIsr, controllerContext.topicReplicaAssignment(topic))
  }

}