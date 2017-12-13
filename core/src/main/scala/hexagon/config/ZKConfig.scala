package hexagon.config

import java.util.Properties

import hexagon.utils.PropKit

class ZKConfig(props: Properties) {


  /**
    * zk host地址
    */
  val zkHost: String = PropKit.getString(props, "zk.host", null)

  /**
    * zookeeper session 过期时间
    */
  val zkSessionTimeoutMs = PropKit.getInt(props, "zk.session.timeout.ms", 6000)

  /**
    * 和zookeeper建立连接时的最长等待时间
    */
  val zkConnectionTimeoutMs = PropKit.getInt(props, "zk.connection.timeout.ms", zkSessionTimeoutMs)

  /** how far a ZK follower can be behind a ZK leader */
  val zkSyncTimeMs = PropKit.getInt(props, "zk.sync.time.ms", 2000)

}
