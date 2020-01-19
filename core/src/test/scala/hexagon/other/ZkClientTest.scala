package hexagon.other

import java.util.concurrent.TimeUnit

import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.recipes.cache.NodeCache
import org.apache.curator.retry.ExponentialBackoffRetry

object ZkClientTest extends App {

	val connectionString = "172.23.7.168:2181,172.23.7.169:2181,172.23.7.170:2181"
	val retryPolicy = new ExponentialBackoffRetry(1000, 3)
	val connectionTimeoutMs = 60 * 1000
	val sessionTimeoutMs = 60 * 1000

	val client = CuratorFrameworkFactory.builder()
		.connectString(connectionString)
		.retryPolicy(retryPolicy)
		.connectionTimeoutMs(connectionTimeoutMs)
		.sessionTimeoutMs(sessionTimeoutMs)
		.build()

	client.start()

	val cache = new NodeCache(client, "/zy/test")

	cache.getListenable().addListener(() => println("----" + cache.getCurrentData.toString))


	cache.start(true)

	TimeUnit.HOURS.sleep(1L)
}
