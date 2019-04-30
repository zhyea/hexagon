package hexagon.other

import java.util.concurrent.TimeUnit

import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.framework.state.{ConnectionState, ConnectionStateListener}
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.curator.framework.state.ConnectionState._

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


  client.getConnectionStateListenable.addListener(new Listener)


  class Listener extends ConnectionStateListener {

    override def stateChanged(client: CuratorFramework, newState: ConnectionState): Unit = {
      newState match {
        case CONNECTED => println("------------------------------------------------------ this is connected")
        case SUSPENDED => println("------------------------------------------------------ this is suspended")
        case RECONNECTED => println("------------------------------------------------------ this is reconnected")
        case LOST => println("------------------------------------------------------ this is lost")
        case READ_ONLY => println("------------------------------------------------------ this is read only")

      }
    }
  }


  TimeUnit.HOURS.sleep(1L)

}
