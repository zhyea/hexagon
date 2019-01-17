package hexagon.other

import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry

object ZkClientTest extends App {


  val connectionString = ""
  val retryPolicy = new ExponentialBackoffRetry(1000, 3)
  val connectionTimeoutMs = 60 * 1000
  val sessionTimeoutMs = 60 * 1000

  val client = CuratorFrameworkFactory.builder()
    .connectString(connectionString)
    .retryPolicy(retryPolicy)
    .connectionTimeoutMs(connectionTimeoutMs)
    .sessionTimeoutMs(sessionTimeoutMs)
    .build();



}
