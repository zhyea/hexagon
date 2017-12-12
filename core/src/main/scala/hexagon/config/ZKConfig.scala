package hexagon.config

import java.util.Properties

import hexagon.utils.PropKit

class ZKConfig(props: Properties) {


  val zkHost = PropKit.getString(props, "zk.host", null)

  val zkSessionTimeoutMs = PropKit.getInt(props, "zk.session.timeout.ms", 6000)

  /** the max time that the client waits to establish a connection to zookeeper */
  val zkConnectionTimeoutMs = PropKit.getInt(props, "zk.connection.timeout.ms", zkSessionTimeoutMs)

  /** how far a ZK follower can be behind a ZK leader */
  val zkSyncTimeMs = PropKit.getInt(props, "zk.sync.time.ms", 2000)

}
