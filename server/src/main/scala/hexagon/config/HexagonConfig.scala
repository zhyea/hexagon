package hexagon.config

import java.util.Properties
import java.util.concurrent.TimeUnit

import hexagon.utils.PropKit._

private[hexagon] class HexagonConfig(props: Properties) {


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
  val bfDir: String = getString(props, "bf.dir")

  val bfValidHours: Int = getInt(props, "bf.valid.hours", 24)

  val bfExpectInsertions: Long = getPositiveLong(props, "bf.expect.insertions", Int.MaxValue)

  val bloomFilterFalsePositiveProbability: Double = getDouble(props, "bf.false.positive.probability", 0.0000000001)

  val bloomFilterBackupRetentionHours: Int = getInt(props, "bf.backup.retention.hours", 24 * 30)


  /**
    * Log Configs
    */
  val enableAof: Boolean = getBool(props, "enable.aof")

  val logDir: String = s"${getString(props, "log.dir")}/$brokerId"

  val logFileSize: Long = getLong(props, "log.file.size", 1 * 1024 * 1024 * 1024)

  val logCleanupIntervalMinutes: Int = getInt(props, "log.cleanup.interval.minutes", 10)

  val logCleanupIntervalMs: Long = TimeUnit.MINUTES.toMillis(logCleanupIntervalMinutes)

  val logRetentionHours: Int = getInt(props, "log.retention.hours", 24 * 7)

  val logRetentionMs: Long = TimeUnit.HOURS.toMillis(logRetentionHours)

  val logRetentionSize: Long = getLong(props, "log.retention.size", -1)

  val logFlushSchedulerIntervalMs: Long = getLong(props, "log.flush.scheduler.interval.ms", 3000)

  val logFlushIntervalMs: Long = getLong(props, "log.flush.interval.ms", 3000)


  /**
    * Zookeeper Configs
    */
  val enableZooKeeper: Boolean = getBool(props, "enable.zookeeper")

  val zkConnect: String = getString(props, "zk.connect")

  val zkSessionTimeout: Int = getInt(props, "zk.session.timeout.ms", 6000)

  val zkConnectionTimeout: Int = getInt(props, "zk.connection.timeout.ms", zkSessionTimeout)

  val zkConnectRetryIntervalMs: Int = getInt(props, "zk.connect.retry.interval.ms", 6000)


}

