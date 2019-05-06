package hexagon.controller

import java.util.concurrent.locks.ReentrantLock

import hexagon.cluster.Broker
import hexagon.zookeeper.ZkClient

import scala.collection.{Set, mutable}

class ControllerContext(val zkClient: ZkClient,
                        val zkSessionTimeout: Int) {

  val controllerLock: ReentrantLock = new ReentrantLock()

  var epoch: Int = 0


  var shuttingDownBrokerIds: mutable.Set[Int] = mutable.Set.empty
  /**
    * topic->replica映射集合
    */
  var topicReplicaAssignment: mutable.Map[String, Seq[Int]] = mutable.Map.empty
  /**
    * topic->leader信息映射
    */
  var topicLeadershipInfo: mutable.Map[String, LeaderIsrAndControllerEpoch] = mutable.Map.empty
  /**
    * 正在执行reassign的 topic->newReplicas 映射
    */
  var topicBeingReassigned: mutable.Map[String, Seq[Int]] = new mutable.HashMap


  private var liveBrokersUnderlying: Set[Broker] = Set.empty
  private var liveBrokerIdsUnderlying: Set[Int] = Set.empty


  def setLiveBrokers(brokers: Set[Broker]): Unit = {
    liveBrokersUnderlying = brokers
    liveBrokerIdsUnderlying = liveBrokersUnderlying.map(_.id)
  }

  def liveBrokers: Set[Broker] = liveBrokersUnderlying.filter(broker => !shuttingDownBrokerIds.contains(broker.id))

  def liveBrokerIds: Set[Int] = liveBrokerIdsUnderlying.diff(shuttingDownBrokerIds)

  def liveOrShuttingDownBrokerIds: Set[Int] = liveBrokerIdsUnderlying

  def liveOrShuttingDownBrokers: Set[Broker] = liveBrokersUnderlying

  def replicaOnBroker(brokerId:Int) =
    topicReplicaAssignment.filter(e=>e._2.contains(brokerId)).map(new TopicAnd)
}
