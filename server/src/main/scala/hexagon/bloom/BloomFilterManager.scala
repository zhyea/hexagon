package hexagon.bloom

import java.io.Closeable
import java.nio.charset.StandardCharsets

import com.google.common.hash.{BloomFilter, Funnels}
import hexagon.config.HexagonConfig
import hexagon.tools.Pool


private[hexagon] class BloomFilterManager(val config: HexagonConfig) extends Closeable {


  val map: Pool[String, BloomFilter[String]] = new Pool()


  def getBloomFilter(topic: String): BloomFilter[String] = {

    def create: BloomFilter[String] = {
      println("zzzzz------------------------")

      BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8),
        config.bloomFilterExpectInsertions,
        config.bloomFilterFalsePositiveProbability)
    }

    map.putIfNotExists(topic, create)
  }


  override def close(): Unit = ???
}
