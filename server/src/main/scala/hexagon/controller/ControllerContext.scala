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

  var topicReplicaAssignment: mutable.Map[String, Seq[Int]] = mutable.Map.empty

  var topicLeadershipInfo: mutable.Map[String, LeaderIsrAndControllerEpoch] = mutable.Map.empty


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


}
