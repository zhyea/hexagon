package hexagon.zookeeper

import java.nio.charset.StandardCharsets

import hexagon.cluster.LeaderAndIsr
import hexagon.config.ZooKeeperConfig
import hexagon.tools.Logging
import hexagon.utils.JSON
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.recipes.cache.{NodeCache, PathChildrenCache}
import org.apache.curator.framework.state.ConnectionStateListener
import org.apache.curator.retry.RetryForever
import org.apache.zookeeper.CreateMode.EPHEMERAL
import org.apache.zookeeper.KeeperException._

object ZkClient extends Logging {

  val BrokerIdsPath: String = "/brokers/ids"

  val BrokerTopicsPath: String = "/brokers/topics"

}


class ZkClient(val config: ZooKeeperConfig) extends Logging {

  import ZkClient._

  private val client = CuratorFrameworkFactory.builder()
    .connectString(config.zkConnect)
    .retryPolicy(new RetryForever(config.zkConnectRetryIntervalMs))
    .connectionTimeoutMs(config.zkConnectionTimeout)
    .sessionTimeoutMs(config.zkSessionTimeout)
    .namespace(config.zkNamespace)
    .build()


  /**
    * 启动zkClient
    */
  def startup(): Unit = {
    info(s"Connecting to zk: ${config.zkConnect}")
    client.start()
  }


  /**
    * 关闭zkClient
    */
  def close(): Unit = {
    info(s"Closing connection to zk:${config.zkConnect}")
    client.close()
  }


  /**
    * 创建父节点
    */
  def createParentPathIfNeeded(path: String): Unit =
    client.checkExists().creatingParentContainersIfNeeded().forPath(path)


  /**
    * 创建临时节点
    */
  def createEphemeralPath(path: String, data: String): Unit = {
    try {
      createEphemeralPath0(path, data)
    } catch {
      case e: NoNodeException => {
        createParentPathIfNeeded(path)
        createEphemeralPath0(path, data)
      }
    }
  }

  private def createEphemeralPath0(path: String, data: String): Unit =
    client.create().withMode(EPHEMERAL).forPath(path, data.getBytes(StandardCharsets.UTF_8))


  /**
    * 创建一个临时节点，如果路径已存在，则抛出NodeExistException异常
    */
  def createEphemeralPathExpectConflict(path: String, data: String): Unit = {
    try {
      createEphemeralPath(path, data)
    } catch {
      case e: NodeExistsException => {
        var storedData: String = null
        try {
          storedData = readData(path)
        } catch {
          case e0: NodeExistsException => //节点消失，由调用方处理异常
          case e1 => throw e1
        }
        if (null != storedData && storedData != data) {
          info(s"Conflict in $path, data: $data, stored data: $storedData")
          throw e
        } else {
          info(s"$path exists with value $data during connection loss; this is ok")
        }
      }
      case e1 => throw e1
    }
  }


  /**
    * 创建一个临时节点，如出现NodeExistException异常，则尝试进行解决
    */
  def createEphemeralPathExpectConflictHandleZKBug(path: String, data: String, expectedCallerData: Any, checker: (String, Any) => Boolean, backoffTime: Int): Unit = {
    while (true) {
      try {
        createEphemeralPathExpectConflict(path, data)
        return
      } catch {
        case e: NodeExistsException => {
          readDataMaybeNull(path) match {
            case Some(data) => {
              if (checker(data, expectedCallerData)) {
                info(s"Conflict occurred at ephemeral node $data at $path a while back in a different session, hence this node will be backoff to be deleted by ZooKeeper and retry")
                Thread.sleep(backoffTime)
              } else {
                throw e
              }
            }
            case None =>
          }
        }
        case e1: Throwable => throw e1
      }
    }
  }


  /**
    * 读取ZooKeeper节点数据
    */
  def readData(path: String): String = {
    val data = client.getData.forPath(path)
    new String(data, StandardCharsets.UTF_8)
  }


  /**
    * 读取ZooKeeper节点数据，并处理节点可能不存在的情况
    */
  def readDataMaybeNull(path: String): Option[String] = {
    try {
      Some(readData(path))
    } catch {
      case e: NoNodeException => None
      case e2: Throwable => throw e2
    }
  }


  /**
    * 创建NodeCache
    */
  def createNodeCache(path: String): NodeCache = {
    new NodeCache(client, path)
  }


  /**
    * 创建PathChildrenCache
    */
  def createPathChildrenCache(path: String, cacheData: Boolean): PathChildrenCache = {
    new PathChildrenCache(client, path, cacheData)
  }


  /**
    * 删除路径
    */
  def deletePath(path: String): Boolean = {
    try {
      client.delete().forPath(path)
      true
    } catch {
      case e: NoNodeException =>
        info(s"$path deleted during connection loss; this is ok")
        false
      case e1: Throwable => throw e1
    }
  }


  /**
    * 添加ZK连接状态监听器
    */
  def registerConnectionStateListener(listener: ConnectionStateListener): Unit = {
    client.getConnectionStateListenable.addListener(listener)
  }


  /**
    * 获取zk中的topic路径
    */
  def getTopicPath(topic: String): String = BrokerTopicsPath + "/" + topic


  /**
    * 读取每个topic的leader和isr信息
    */
  def getLeaderAndIsrForTopic(topic: String): Option[LeaderAndIsr] = {
    val leaderAndIsrOpt = readDataMaybeNull(getTopicPath(topic))
    leaderAndIsrOpt match {
      case Some(leaderAndIsrStr) => JSON.fromJson(leaderAndIsrStr, classOf[LeaderAndIsr])
      case None => None
    }
  }

}
