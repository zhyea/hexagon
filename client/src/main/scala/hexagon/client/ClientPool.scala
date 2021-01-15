package hexagon.client

import java.util.Properties
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}

import hexagon.cluster.{Broker, Topic}
import hexagon.config.ClientConfig
import hexagon.exceptions.{InvalidConfigException, UnavailableClientException}
import hexagon.serializer.Serializer

class ClientPool[T](props: Properties,
					serializer: Serializer[T],
					syncClients: ConcurrentMap[Integer, SyncClient]) {


	if (null == serializer)
		throw new InvalidConfigException("Serializer is null!")


	def this(props: Properties, serializer: Serializer[T]) =
		this(props, serializer, new ConcurrentHashMap[Integer, SyncClient]())


	def addProducer(broker: Broker): Unit = {
		props.put("host", broker.host)
		props.put("port", broker.port)

		val client = new SyncClient(new ClientConfig(props))
		syncClients.put(broker.id, client)
	}


	def close(): Unit = {
		syncClients.values().forEach(_.close())
	}


	def send(topic: Topic, data: Seq[T]): List[Short] = {
		val client: SyncClient = syncClients.get(topic.brokerId)
		if (null == client)
			throw new UnavailableClientException()

		val response = client.send(topic.topicName, data.map(serializer.toMessage): _*)
		response.msgStates
	}


}
