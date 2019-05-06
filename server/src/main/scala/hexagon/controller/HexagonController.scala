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


  /**
    * This callback is invoked by the replica state machine's broker change listener, with the list of newly started
    * brokers as input. It does the following -
    * 1. Triggers the OnlinePartition state change for all new/offline partitions
    * 2. It checks whether there are reassigned replicas assigned to any newly started brokers.  If
    *    so, it performs the reassignment logic for each topic/partition.
    *
    * Note that we don't need to refresh the leader/isr cache for all topic/partitions at this point for two reasons:
    * 1. The partition state machine, when triggering online state change, will refresh leader and ISR for only those
    *    partitions currently new or offline (rather than every partition this controller is aware of)
    * 2. Even if we do refresh the cache, there is no guarantee that by the time the leader and ISR request reaches
    *    every broker that it is still valid.  Brokers check the leader epoch to determine validity of the request.
    */
  def onBrokerStartup(newBrokerId: Int) {
    info(s"New broker startup callback for $newBrokerId")
    val newBrokersSet = newBrokers.toSet
    // send update metadata request for all partitions to the newly restarted brokers. In cases of controlled shutdown
    // leaders will not be elected when a new broker comes up. So at least in the common controlled shutdown case, the
    // metadata will reach the new brokers faster
    sendUpdateMetadataRequest(newBrokers)
    // the very first thing to do when a new broker comes up is send it the entire list of partitions that it is
    // supposed to host. Based on that the broker starts the high watermark threads for the input list of partitions
    val allReplicasOnNewBrokers = controllerContext.replicasOnBrokers(newBrokersSet)
    replicaStateMachine.handleStateChanges(allReplicasOnNewBrokers, OnlineReplica)
    // when a new broker comes up, the controller needs to trigger leader election for all new and offline partitions
    // to see if these brokers can become leaders for some/all of those
    partitionStateMachine.triggerOnlinePartitionStateChange()
    // check if reassignment of some partitions need to be restarted
    val partitionsWithReplicasOnNewBrokers = controllerContext.partitionsBeingReassigned.filter {
      case (topicAndPartition, reassignmentContext) => reassignmentContext.newReplicas.exists(newBrokersSet.contains(_))
    }
    partitionsWithReplicasOnNewBrokers.foreach(p => onPartitionReassignment(p._1, p._2))
    // check if topic deletion needs to be resumed. If at least one replica that belongs to the topic being deleted exists
    // on the newly restarted brokers, there is a chance that topic deletion can resume
    val replicasForTopicsToBeDeleted = allReplicasOnNewBrokers.filter(p => deleteTopicManager.isTopicQueuedUpForDeletion(p.topic))
    if(replicasForTopicsToBeDeleted.size > 0) {
      info(("Some replicas %s for topics scheduled for deletion %s are on the newly restarted brokers %s. " +
        "Signaling restart of topic deletion for these topics").format(replicasForTopicsToBeDeleted.mkString(","),
        deleteTopicManager.topicsToBeDeleted.mkString(","), newBrokers.mkString(",")))
      deleteTopicManager.resumeDeletionForTopics(replicasForTopicsToBeDeleted.map(_.topic))
    }
  }

  /**
    * This callback is invoked by the replica state machine's broker change listener with the list of failed brokers
    * as input. It does the following -
    * 1. Mark partitions with dead leaders as offline
    * 2. Triggers the OnlinePartition state change for all new/offline partitions
    * 3. Invokes the OfflineReplica state change on the input list of newly started brokers
    *
    * Note that we don't need to refresh the leader/isr cache for all topic/partitions at this point.  This is because
    * the partition state machine will refresh our cache for us when performing leader election for all new/offline
    * partitions coming online.
    */
  def onBrokerFailure(deadBrokers: Seq[Int]) {
    info("Broker failure callback for %s".format(deadBrokers.mkString(",")))
    val deadBrokersThatWereShuttingDown =
      deadBrokers.filter(id => controllerContext.shuttingDownBrokerIds.remove(id))
    info("Removed %s from list of shutting down brokers.".format(deadBrokersThatWereShuttingDown))
    val deadBrokersSet = deadBrokers.toSet
    // trigger OfflinePartition state for all partitions whose current leader is one amongst the dead brokers
    val partitionsWithoutLeader = controllerContext.partitionLeadershipInfo.filter(partitionAndLeader =>
      deadBrokersSet.contains(partitionAndLeader._2.leaderAndIsr.leader) &&
        !deleteTopicManager.isTopicQueuedUpForDeletion(partitionAndLeader._1.topic)).keySet
    partitionStateMachine.handleStateChanges(partitionsWithoutLeader, OfflinePartition)
    // trigger OnlinePartition state changes for offline or new partitions
    partitionStateMachine.triggerOnlinePartitionStateChange()
    // filter out the replicas that belong to topics that are being deleted
    var allReplicasOnDeadBrokers = controllerContext.replicasOnBrokers(deadBrokersSet)
    val activeReplicasOnDeadBrokers = allReplicasOnDeadBrokers.filterNot(p => deleteTopicManager.isTopicQueuedUpForDeletion(p.topic))
    // handle dead replicas
    replicaStateMachine.handleStateChanges(activeReplicasOnDeadBrokers, OfflineReplica)
    // check if topic deletion state for the dead replicas needs to be updated
    val replicasForTopicsToBeDeleted = allReplicasOnDeadBrokers.filter(p => deleteTopicManager.isTopicQueuedUpForDeletion(p.topic))
    if(replicasForTopicsToBeDeleted.size > 0) {
      // it is required to mark the respective replicas in TopicDeletionFailed state since the replica cannot be
      // deleted when the broker is down. This will prevent the replica from being in TopicDeletionStarted state indefinitely
      // since topic deletion cannot be retried until at least one replica is in TopicDeletionStarted state
      deleteTopicManager.failReplicaDeletion(replicasForTopicsToBeDeleted)
    }
  }

  /**
    * This callback is invoked by the partition state machine's topic change listener with the list of new topics
    * and partitions as input. It does the following -
    * 1. Registers partition change listener. This is not required until KAFKA-347
    * 2. Invokes the new partition callback
    * 3. Send metadata request with the new topic to all brokers so they allow requests for that topic to be served
    */
  def onNewTopicCreation(topics: Set[String], newPartitions: Set[TopicAndPartition]) {
    info("New topic creation callback for %s".format(newPartitions.mkString(",")))
    // subscribe to partition changes
    topics.foreach(topic => partitionStateMachine.registerPartitionChangeListener(topic))
    onNewPartitionCreation(newPartitions)
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
  def sendUpdateMetadataRequest(broker: Int) {
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
