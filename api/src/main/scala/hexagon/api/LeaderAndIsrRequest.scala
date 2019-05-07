package hexagon.api

import java.nio.ByteBuffer

import hexagon.cluster.{Broker, LeaderAndIsr}
import hexagon.tools.BYTES
import hexagon.utils.IOUtils._

import scala.collection.mutable


object LeaderAndIsrRequest {

  def readFrom(buffer: ByteBuffer): LeaderAndIsrRequest = {
    val correlationId = buffer.getInt
    val clientId = readShortString(buffer)
    val controllerId = buffer.getInt
    val controllerEpoch = buffer.getInt
    val topicStateInfosCount = buffer.getInt

    val topicStateInfos = new mutable.HashMap[String, TopicStateInfo]
    for (i <- 0 until topicStateInfosCount) {
      val topic = readShortString(buffer)
      val topicStateInfo = TopicStateInfo.readFrom(buffer)
      topicStateInfos.put(topic, topicStateInfo)
    }

    val leadersCount = buffer.getInt
    var leaders = Set[Broker]()
    for (i <- 0 until leadersCount) {
      leaders += Broker.readFrom(buffer)
    }
    new LeaderAndIsrRequest(correlationId, clientId, controllerId, controllerEpoch, topicStateInfos.toMap, leaders)
  }


}


case class LeaderAndIsrRequest(correlationId: Int,
                               clientId: String,
                               controllerId: Int,
                               controllerEpoch: Int,
                               topicStateInfos: Map[String, TopicStateInfo],
                               leaders: Set[Broker]) extends RequestOrResponse(RequestKeys.LeaderAndIsr) {

  override def sizeInBytes: Int = {
    var size =
      BYTES.Int + // correlation id
        shortStringLength(clientId) + // client id
        BYTES.Int + // controller id
        BYTES.Int + // controller epoch
        BYTES.Int // number of topics

    for ((k, v) <- topicStateInfos) {
      size += shortStringLength(k) /*topic*/ + v.sizeInBytes // topic state info
    }

    size += BYTES.Int // number of leader brokers

    for (broker <- leaders) {
      size += broker.sizeInBytes // broker info
    }

    size
  }

  override def writeTo(buffer: ByteBuffer): Unit = {
    buffer.putInt(correlationId)

    writeShortString(buffer, clientId)

    buffer.putInt(controllerId)
    buffer.putInt(controllerEpoch)
    buffer.putInt(topicStateInfos.size)

    for ((k, v) <- topicStateInfos) {
      writeShortString(buffer, k)
      v.writeTo(buffer)
    }

    buffer.putInt(controllerId)
    buffer.putInt(controllerId)

    leaders.foreach(_.writeTo(buffer))
  }

}


/* TopicStateInfo */

object TopicStateInfo {

  def readFrom(buffer: ByteBuffer): TopicStateInfo = {
    val controllerEpoch = buffer.getInt
    val leader = buffer.getInt
    val leaderEpoch = buffer.getInt
    val isrSize = buffer.getInt
    val isr = for (i <- 0 until isrSize) yield buffer.getInt
    val zkVersion = buffer.getInt
    val replicationFactor = buffer.getInt
    val replicas = for (i <- 0 until replicationFactor) yield buffer.getInt

    TopicStateInfo(LeaderAndIsr(leader, leaderEpoch, isr.toList, zkVersion), controllerEpoch, replicas.toSet)
  }


}


case class TopicStateInfo(leaderAndIsr: LeaderAndIsr,
                          controllerEpoch: Int,
                          allReplicas: Set[Int]) {

  def replicationFactor: Int = allReplicas.size

  def writeTo(buffer: ByteBuffer): Unit = {
    buffer.putInt(controllerEpoch)
    buffer.putInt(leaderAndIsr.leader)
    buffer.putInt(leaderAndIsr.leaderEpoch)
    buffer.putInt(leaderAndIsr.isr.size)

    leaderAndIsr.isr.foreach(buffer.putInt)

    buffer.putInt(leaderAndIsr.zkVersion)
    buffer.putInt(replicationFactor)

    allReplicas.foreach(buffer.putInt)
  }


  def sizeInBytes: Int = {
    val size: Int =
      BYTES.Int + // controller epoch
        BYTES.Int + // leader broker id
        BYTES.Int + // leader epoch
        BYTES.Int + // leader isr size
        BYTES.Int * leaderAndIsr.isr.size + //replicas in isr
        BYTES.Int + // zk version
        BYTES.Int + // replication factor
        BYTES.Int * allReplicas.size
    size
  }


  override def toString: String = {
    val builder = new StringBuilder
    builder.append("(LeaderAndIsr:" + leaderAndIsr.toString)
    builder.append(",controllerEpoch:" + controllerEpoch)
    builder.append(",ReplicationFactor:" + replicationFactor)
    builder.append(",AllReplicas:" + allReplicas.mkString(",") + ")")

    builder.toString()
  }

}
