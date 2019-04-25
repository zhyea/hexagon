package hexagon.zookeeper

import java.nio.charset.StandardCharsets

import hexagon.config.ZooKeeperConfig
import hexagon.tools.Logging
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.RetryForever
import org.apache.zookeeper.CreateMode.EPHEMERAL
import org.apache.zookeeper.KeeperException._

object ZkClient extends Logging {

  val BrokerIdsPath: String = "/brokers/ids"

  val BrokerTopicsPath: String = "/brokers/topics"

}


/**
  *
  */
class ZkClient(val config: ZooKeeperConfig) extends Logging {

  private val client = CuratorFrameworkFactory.builder()
    .connectString(config.zkConnect)
    .retryPolicy(new RetryForever(config.zkConnectRetryIntervalMs))
    .connectionTimeoutMs(config.zkConnectionTimeout)
    .sessionTimeoutMs(config.zkSessionTimeout)
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
  def createParentPath(path: String): Unit =
    client.checkExists().creatingParentContainersIfNeeded().forPath(path)


  /**
    * 创建临时节点
    */
  def createEphemeralPath(path: String, data: String): Unit = {
    try {
      createEphemeralPath0(path, data)
    } catch {
      case e: NoNodeException => {
        createParentPath(path)
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
}