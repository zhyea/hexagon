package hexagon.network

import java.nio.charset.StandardCharsets

import hexagon.tools.Logging
import org.apache.curator.framework.CuratorFramework
import org.apache.zookeeper.CreateMode._
import org.apache.zookeeper.KeeperException.{NoNodeException, NodeExistsException}

object ZkTools extends Logging {

  val BrokerIdsPath: String = "/brokers/ids"

  val BrokerTopicsPath: String = "/brokers/topics"

  /**
    * 创建父节点
    */
  def createParentPath(client: CuratorFramework, path: String): Unit = {
    client.checkExists()
      .creatingParentContainersIfNeeded()
      .forPath(path)
  }


  /**
    * 创建临时节点
    */
  def createEphemeralPath(client: CuratorFramework, path: String, data: String): Unit = {
    try {
      createEphemeralPath0(client, path, data)
    } catch {
      case e: NoNodeException => {
        createParentPath(client, path)
        createEphemeralPath0(client, path, data)
      }
    }
  }


  private def createEphemeralPath0(client: CuratorFramework, path: String, data: String): Unit = {
    client.create().withMode(EPHEMERAL).forPath(path, data.getBytes(StandardCharsets.UTF_8))
  }

  /**
    * 创建一个临时节点，如果路径已存在，则抛出NodeExistException异常
    */
  def createEphemeralPathExpectConflict(client: CuratorFramework, path: String, data: String): Unit = {
    try {
      createEphemeralPath(client, path, data)
    } catch {
      case e: NodeExistsException => {
        var storedData: String = null
        try {
          storedData = readData(client, path)
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

  def readData(client: CuratorFramework, path: String): String = {
    val data = client.getData.forPath(path)
    new String(data, StandardCharsets.UTF_8)
  }

}
