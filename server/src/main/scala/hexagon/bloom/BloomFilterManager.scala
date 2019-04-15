package hexagon.bloom

import java.io.Closeable
import java.nio.charset.StandardCharsets

import com.google.common.hash.{BloomFilter, Funnels}
import hexagon.config.HexagonConfig


private[hexagon] class BloomFilterManager(val config: HexagonConfig) extends Closeable {


  val map: Map[String, BloomFilter[String]] = Map()


  def getBloomFilter(topic: String): BloomFilter[String] = {

    def create: BloomFilter[String] = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8),
      config.bloomFilterExpectInsertions,
      config.bloomFilterFalsePositiveProbability)

    map.getOrElse(topic, create)
  }


  override def close(): Unit = ???
}
