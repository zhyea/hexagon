package hexagon.controller

import java.util.concurrent.atomic.AtomicBoolean

import hexagon.config.HexagonConfig
import hexagon.tools.Logging
import hexagon.utils.Locks._
import hexagon.zookeeper.{LeaderElectListener, ZkClient, ZkLeaderElector}
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.state.{ConnectionState, ConnectionStateListener}

import scala.collection.{Seq, Set}


object HexagonController extends Logging {

  val stateChangeLogger: StateChangeLogger = StateChangeLogger("state.change")

  case class StateChangeLogger(name: String) extends Logging


}


class HexagonController(val config: HexagonConfig,
                        val zkClient: ZkClient,
                        val brokerState: BrokerState) extends Logging {
  private val isRunning: AtomicBoolean = new AtomicBoolean(true)
  private val controllerContext = new ControllerContext(zkClient, config.zkSessionTimeout)
  private val controllerElector = new ZkLeaderElector(config.ControllerPath, controllerContext, new LeaderChangeListener, config.brokerId)

  val clientId = s"id_${config.brokerId}—host_${config.host}-port_${config.port}"


  /**
    * 启动Controller
    */
  def startup(): Unit = {
    inLock(controllerContext.controllerLock) {
      info("Controller starting up")
      registerSessionExpirationListener()
      isRunning.set(true)

    }
  }


  def onResignation(): Unit = {

  }


  private def registerSessionExpirationListener(): Any = {
    ???
  }


  def onPartitionReassignment(topic: String, reassignedReplicas: Seq[Int]) {
    areReplicasInIsr(topic, reassignedReplicas) match {
      case false =>
        info(s"New replicas ${reassignedReplicas.mkString(",")} for topic $topic being reassigned not yet caught up with the leader")
        val newReplicasNotInOldReplicaList = reassignedReplicas.toSet -- controllerContext.topicReplicaAssignment(topic).toSet
        val newAndOldReplicas = (reassignedReplicas  ++ controllerContext.topicReplicaAssignment(topic)).toSet
        //1. Update AR in ZK with OAR + RAR.
        updateAssignedReplicasForTopic(topic, newAndOldReplicas.toSeq)
        //2. Send LeaderAndIsr request to every replica in OAR + RAR (with AR as OAR + RAR).
        updateLeaderEpochAndSendRequest(topic, controllerContext.topicReplicaAssignment(topic),
          newAndOldReplicas.toSeq)
        //3. replicas in RAR - OAR -> NewReplica
        startNewReplicasForReassignedPartition(topic, newReplicas, newReplicasNotInOldReplicaList)
        info("Waiting for new replicas %s for partition %s being ".format(reassignedReplicas.mkString(","), topic) +
          "reassigned to catch up with the leader")
      case true =>
        //4. Wait until all replicas in RAR are in sync with the leader.
        val oldReplicas = controllerContext.topicReplicaAssignment(topic).toSet -- reassignedReplicas.toSet
        //5. replicas in RAR -> OnlineReplica
        reassignedReplicas.foreach { replica =>
          replicaStateMachine.handleStateChanges(Set(new PartitionAndReplica(topic.topic, topic.partition,
            replica)), OnlineReplica)
        }
        //6. Set AR to RAR in memory.
        //7. Send LeaderAndIsr request with a potential new leader (if current leader not in RAR) and
        //   a new AR (using RAR) and same isr to every broker in RAR
        moveReassignedPartitionLeaderIfRequired(topic, newReplicas)
        //8. replicas in OAR - RAR -> Offline (force those replicas out of isr)
        //9. replicas in OAR - RAR -> NonExistentReplica (force those replicas to be deleted)
        stopOldReplicasOfReassignedPartition(topic, newReplicas, oldReplicas)
        //10. Update AR in ZK with RAR.
        updateAssignedReplicasForTopic(topic, reassignedReplicas)
        //11. Update the /admin/reassign_partitions path in ZK to remove this partition.
        removePartitionFromReassignedPartitions(topic)
        info("Removed partition %s from the list of reassigned partitions in zookeeper".format(topic))
        controllerContext.partitionsBeingReassigned.remove(topic)
        //12. After electing leader, the replicas and isr information changes, so resend the update metadata request to every broker
        sendUpdateMetadataRequest(controllerContext.liveOrShuttingDownBrokerIds.toSeq, Set(topic))
        // signal delete topic thread if reassignment for some partitions belonging to topics being deleted just completed
        deleteTopicManager.resumeDeletionForTopics(Set(topic.topic))
    }
  }



  private def updateAssignedReplicasForTopic(topic: String,
                                                 replicas: Seq[Int]) {
    val partitionsAndReplicasForThisTopic = controllerContext.partitionReplicaAssignment.filter(_._1.topic.equals(topic.topic))
    partitionsAndReplicasForThisTopic.put(topic, replicas)
    updateAssignedReplicasForTopic(topic, partitionsAndReplicasForThisTopic)
    info("Updated assigned replicas for partition %s being reassigned to %s ".format(topic, replicas.mkString(",")))
    // update the assigned replica list after a successful zookeeper write
    controllerContext.partitionReplicaAssignment.put(topic, replicas)
  }

  private def startNewReplicasForReassignedPartition(topicAndPartition: TopicAndPartition,
                                                     reassignedPartitionContext: ReassignedPartitionsContext,
                                                     newReplicas: Set[Int]) {
    // send the start replica request to the brokers in the reassigned replicas list that are not in the assigned
    // replicas list
    newReplicas.foreach { replica =>
      replicaStateMachine.handleStateChanges(Set(new PartitionAndReplica(topicAndPartition.topic, topicAndPartition.partition, replica)), NewReplica)
    }
  }

  private def updateLeaderEpochAndSendRequest(topicAndPartition: TopicAndPartition, replicasToReceiveRequest: Seq[Int], newAssignedReplicas: Seq[Int]) {
    brokerRequestBatch.newBatch()
    updateLeaderEpoch(topicAndPartition.topic, topicAndPartition.partition) match {
      case Some(updatedLeaderIsrAndControllerEpoch) =>
        brokerRequestBatch.addLeaderAndIsrRequestForBrokers(replicasToReceiveRequest, topicAndPartition.topic,
          topicAndPartition.partition, updatedLeaderIsrAndControllerEpoch, newAssignedReplicas)
        brokerRequestBatch.sendRequestsToBrokers(controllerContext.epoch, controllerContext.correlationId.getAndIncrement)
        stateChangeLogger.trace(("Controller %d epoch %d sent LeaderAndIsr request %s with new assigned replica list %s " +
          "to leader %d for partition being reassigned %s").format(config.brokerId, controllerContext.epoch, updatedLeaderIsrAndControllerEpoch,
          newAssignedReplicas.mkString(","), updatedLeaderIsrAndControllerEpoch.leaderAndIsr.leader, topicAndPartition))
      case None => // fail the reassignment
        stateChangeLogger.error(("Controller %d epoch %d failed to send LeaderAndIsr request with new assigned replica list %s " +
          "to leader for partition being reassigned %s").format(config.brokerId, controllerContext.epoch,
          newAssignedReplicas.mkString(","), topicAndPartition))
    }
  }



  private def moveReassignedPartitionLeaderIfRequired(topicAndPartition: TopicAndPartition,
                                                      reassignedPartitionContext: ReassignedPartitionsContext) {
    val reassignedReplicas = reassignedPartitionContext.newReplicas
    val currentLeader = controllerContext.partitionLeadershipInfo(topicAndPartition).leaderAndIsr.leader
    // change the assigned replica list to just the reassigned replicas in the cache so it gets sent out on the LeaderAndIsr
    // request to the current or new leader. This will prevent it from adding the old replicas to the ISR
    val oldAndNewReplicas = controllerContext.partitionReplicaAssignment(topicAndPartition)
    controllerContext.partitionReplicaAssignment.put(topicAndPartition, reassignedReplicas)
    if(!reassignedPartitionContext.newReplicas.contains(currentLeader)) {
      info("Leader %s for partition %s being reassigned, ".format(currentLeader, topicAndPartition) +
        "is not in the new list of replicas %s. Re-electing leader".format(reassignedReplicas.mkString(",")))
      // move the leader to one of the alive and caught up new replicas
      partitionStateMachine.handleStateChanges(Set(topicAndPartition), OnlinePartition, reassignedPartitionLeaderSelector)
    } else {
      // check if the leader is alive or not
      controllerContext.liveBrokerIds.contains(currentLeader) match {
        case true =>
          info("Leader %s for partition %s being reassigned, ".format(currentLeader, topicAndPartition) +
            "is already in the new list of replicas %s and is alive".format(reassignedReplicas.mkString(",")))
          // shrink replication factor and update the leader epoch in zookeeper to use on the next LeaderAndIsrRequest
          updateLeaderEpochAndSendRequest(topicAndPartition, oldAndNewReplicas, reassignedReplicas)
        case false =>
          info("Leader %s for partition %s being reassigned, ".format(currentLeader, topicAndPartition) +
            "is already in the new list of replicas %s but is dead".format(reassignedReplicas.mkString(",")))
          partitionStateMachine.handleStateChanges(Set(topicAndPartition), OnlinePartition, reassignedPartitionLeaderSelector)
      }
    }
  }

  private def stopOldReplicasOfReassignedPartition(topicAndPartition: TopicAndPartition,
                                                   reassignedPartitionContext: ReassignedPartitionsContext,
                                                   oldReplicas: Set[Int]) {
    val topic = topicAndPartition.topic
    val partition = topicAndPartition.partition
    // first move the replica to offline state (the controller removes it from the ISR)
    val replicasToBeDeleted = oldReplicas.map(r => PartitionAndReplica(topic, partition, r))
    replicaStateMachine.handleStateChanges(replicasToBeDeleted, OfflineReplica)
    // send stop replica command to the old replicas
    replicaStateMachine.handleStateChanges(replicasToBeDeleted, ReplicaDeletionStarted)
    // TODO: Eventually partition reassignment could use a callback that does retries if deletion failed
    replicaStateMachine.handleStateChanges(replicasToBeDeleted, ReplicaDeletionSuccessful)
    replicaStateMachine.handleStateChanges(replicasToBeDeleted, NonExistentReplica)
  }

  def removePartitionFromReassignedPartitions(topicAndPartition: TopicAndPartition) {
    if(controllerContext.partitionsBeingReassigned.get(topicAndPartition).isDefined) {
      // stop watching the ISR changes for this partition
      zkClient.unsubscribeDataChanges(ZkUtils.getTopicPartitionLeaderAndIsrPath(topicAndPartition.topic, topicAndPartition.partition),
        controllerContext.partitionsBeingReassigned(topicAndPartition).isrChangeListener)
    }
    // read the current list of reassigned partitions from zookeeper
    val partitionsBeingReassigned = ZkUtils.getPartitionsBeingReassigned(zkClient)
    // remove this partition from that list
    val updatedPartitionsBeingReassigned = partitionsBeingReassigned - topicAndPartition
    // write the new list to zookeeper
    ZkUtils.updatePartitionReassignmentData(zkClient, updatedPartitionsBeingReassigned.mapValues(_.newReplicas))
    // update the cache. NO-OP if the partition's reassignment was never started
    controllerContext.partitionsBeingReassigned.remove(topicAndPartition)
  }

  /**
    * Send the leader information for selected partitions to selected brokers so that they can correctly respond to
    * metadata requests
    * @param brokers The brokers that the update metadata request should be sent to
    */
  def sendUpdateMetadataRequest(brokers: Seq[Int], partitions: Set[TopicAndPartition] = Set.empty[TopicAndPartition]) {
    brokerRequestBatch.newBatch()
    brokerRequestBatch.addUpdateMetadataRequestForBrokers(brokers, partitions)
    brokerRequestBatch.sendRequestsToBrokers(epoch, controllerContext.correlationId.getAndIncrement)
  }

  private def areReplicasInIsr(topic: String, replicas: Seq[Int]): Boolean = {
    zkClient.getLeaderAndIsrForTopic(topic) match {
      case Some(leaderAndIsr) =>
        val replicasNotInIsr = replicas.filterNot(leaderAndIsr.isr.contains)
        replicasNotInIsr.isEmpty
      case None => false
    }
  }


  class SessionExpirationListener extends ConnectionStateListener with Logging {

    override def stateChanged(client: CuratorFramework, newState: ConnectionState): Unit = ???

  }


  class LeaderChangeListener extends LeaderElectListener with Logging {

    override def onBecomingLeader(): Unit = ???

    override def onResigningAsLeader(): Unit = ???
  }

}
