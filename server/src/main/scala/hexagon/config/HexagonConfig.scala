package hexagon.config

import java.util.Properties

import hexagon.utils.PropKit._

private[hexagon] class HexagonConfig(props: Properties) extends ZooKeeperConfig(props) {


  /**
    * Server Configs
    */
  val brokerId: Int = getInt(props, "broker.id", -1)

  val port: Int = getInt(props, "port", 8190)

  val host: String = getString(props, "host", "127.0.0.1")

  val numIoThreads: Int = getInt(props, "num.io.threads", Int.MaxValue)

  val socketSendBuffer: Int = getInt(props, "socket.send.buffer.bytes", 1024)

  val socketReceiveBuffer: Int = getInt(props, "socket.receive.buffer.bytes", 1024)

  val maxSocketRequestSize: Int = getInt(props, "socket.request.max.bytes", Int.MaxValue)


  /**
    * BloomFilter Configs
    */
  val bloomFilterValidHours: Int = getInt(props, "bf.valid.hours", 24);

  val bloomFilterExpectInsertions: Long =
    if (getLong(props, "bf.expect.insertions") <= 0) Int.MaxValue else getLong(props, "bf.expect.insertions")


  val bloomFilterFalsePositiveProbability: Double = getDouble(props, "bf.false.positive.probability", 0.0000000001)

  val bloomFilterBackupRetentionHours: Int = getInt(props, "bf.backup.retention.hours", 24 * 30)


  /**
    * Zookeeper Configs
    */


}
