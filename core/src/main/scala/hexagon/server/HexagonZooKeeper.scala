package hexagon.server

import java.net.InetAddress

import hexagon.cluster.Broker
import hexagon.config.HexagonConfig
import hexagon.utils.{Logging, ZKUtils, ZkStringSerializer}
import org.I0Itec.zkclient.exception.ZkNodeExistsException
import org.I0Itec.zkclient.{IZkStateListener, ZkClient}
import org.apache.zookeeper.Watcher.Event

class HexagonZooKeeper(val config: HexagonConfig) extends Logging {

	val brokerIdPath = config.brokerIdsPath + "/" + config.brokerId

	var zkClient: ZkClient = null

	val lock = new Object()

	def startup() {
		info("Connecting to ZK: {}.", config.zkHost)
		zkClient = new ZkClient(config.zkHost,
			config.zkSessionTimeoutMs,
			config.zkConnectionTimeoutMs,
			ZkStringSerializer)

		zkClient.subscribeStateChanges(new SessionExpireListener)
	}


	def registerBrokerInZk(): Unit = {
		info("Registering broker:{}. ", brokerIdPath)
		val hostName = if (null == config.hostName) InetAddress.getLocalHost.getHostAddress else config.hostName
		val creatorId: String = hostName + "-" + System.currentTimeMillis()
		val broker = new Broker(config.brokerId, creatorId, hostName, config.port)
		try {
			ZKUtils.createEphemeralPathExpectConflict(zkClient, brokerIdPath, broker.getZkString())
		} catch {
			case e: ZkNodeExistsException => info("A broker is already registered on the path {}.", brokerIdPath)
		}
		info("Registering broker {} succeeded with {}", brokerIdPath, broker)
	}


	def registerTopicInZkInternal(topic: String): Unit = {

	}


	/**
	  * When we get a SessionExpire event, we lost all ephemeral nodes and zkClient has reestablished a connect for us.
	  * We need to re-register the broker in the broker registry.
	  */
	class SessionExpireListener extends IZkStateListener with Logging {

		override def handleSessionEstablishmentError(error: Throwable): Unit = ???

		override def handleStateChanged(state: Event.KeeperState): Unit = ???


		/**
		  * Called after the ZooKeeper session has expired and a new session has been created.
		  * You would have to re-create any ephemeral nodes here.
		  */
		override def handleNewSession(): Unit = {
			info("Re-registering broker info in ZK for broker {}.", config.brokerId)

		}
	}

}