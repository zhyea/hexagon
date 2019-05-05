package hexagon.controller

import hexagon.utils.Locks._
import hexagon.zookeeper.{PathChildrenListener, ZkClient}
import org.apache.curator.framework.recipes.cache.{ChildData, PathChildrenCache}


class ReassignedTopicIsrChangeListener(cache: PathChildrenCache,
                                       zkClient: ZkClient,
                                       controllerContext: ControllerContext,
                                       onTopicReassigned: (String, Seq[Int]) => Unit) extends PathChildrenListener(cache) {


  override def onDataChange(data: ChildData): Unit = {

    inLock(controllerContext.controllerLock) {
      val topic = readTopic(data.getPath)
      val reassignedReplicasOpt = controllerContext.topicBeingReassigned.get(topic)
      reassignedReplicasOpt match {
        case Some(reassignedReplicas) =>
          val newLeaderAndIsrOpt = zkClient.getLeaderAndIsrForTopic(topic)
          newLeaderAndIsrOpt match {
            case Some(leaderAndIsr) =>
              // 检查新添加的replica是否已加入了isr
              val caughtUpReplicas = reassignedReplicas.toSet & leaderAndIsr.isr.toSet
              if (caughtUpReplicas == reassignedReplicas.toSet) {
                // 如已全部加入isr，继续执行topic replica分配任务
                info(s"${caughtUpReplicas.size}/${reassignedReplicas.size} replicas have caught up with the leader for topic $topic being reassigned. " +
                  "Resuming partition reassignment")
                onTopicReassigned(topic, reassignedReplicas)
              } else {
                info(s"${caughtUpReplicas.size}/${reassignedReplicas.size} replicas have caught up with the leader for topic $topic being reassigned." +
                  s"Replica(s) ${(reassignedReplicas.toSet -- leaderAndIsr.isr.toSet).mkString(",")} still need to catch up")
              }

            case None => error(s"Error handling reassignment of topic $topic to replicas ${reassignedReplicas.mkString(",")} as it was never created")
          }

        case None =>
      }

    }
  }


  def readTopic(path: String): String = {
    path.substring(path.lastIndexOf("/"))
  }

}
