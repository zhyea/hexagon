package hexagon.server

import java.net.InetAddress

import hexagon.cluster.Broker
import hexagon.config.HexagonConfig
import hexagon.log.LogManager
import hexagon.network.ZkTools._
import hexagon.tools.Logging
import hexagon.utils.JSON
import hexagon.utils.StrUtils._
import org.apache.curator.framework.recipes.cache.NodeCacheListener
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.RetryForever
import org.apache.zookeeper.KeeperException.NodeExistsException


/**
  * Hexagon与ZooKeeper交互类
  */
class HexagonZooKeeper(config: HexagonConfig, logManager: LogManager) extends Logging {

  private val brokerIdPath: String = BrokerIdsPath + "/" + config.brokerId
  private var zkClient: CuratorFramework = null

  private var topics: List[String] = Nil
  private val lock = new Object()


  def startup(): Unit = {
    info(s"Connecting to zk: ${config.zkConnect}")

    zkClient = CuratorFrameworkFactory.builder()
      .connectString(config.zkConnect)
      .retryPolicy(new RetryForever(config.zkConnectRetryIntervalMs))
      .connectionTimeoutMs(config.zkConnectionTimeout)
      .sessionTimeoutMs(config.zkSessionTimeout)
      .build()

    zkClient.start()
  }


  /**
    * 在zk中注册Broker信息
    */
  def registerBrokerInZk(): Unit = {
    info(s"Registering broker $brokerIdPath")
    val hostName = if (isBlank(config.host)) InetAddress.getLocalHost.getHostAddress else config.host
    val broker = new Broker(config.brokerId, hostName, config.port)

    try {
      createEphemeralPath(zkClient, brokerIdPath, JSON.toJson(broker))
    } catch {
      case e: NodeExistsException => throw new IllegalStateException(s"A broker is already registered on the path '$brokerIdPath'.")
    }
    info(s"Registering broker $brokerIdPath succeeded with $broker")
  }


  /**
    * 在zk中注册topic信息
    */
  def registerTopicInZk(topic: String): Unit = {


  }


  def registerTopicInZkInternal(topic: String): Unit = {
    val brokerTopicPath = BrokerTopicsPath + "/" + topic + config.brokerId

  }


  class SessionExpireListener extends NodeCacheListener {
    override def nodeChanged(): Unit = ???
  }


}
