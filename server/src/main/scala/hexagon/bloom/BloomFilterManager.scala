package hexagon.bloom

import java.io.Closeable
import java.nio.charset.StandardCharsets

import com.google.common.hash.{BloomFilter, Funnels}
import hexagon.config.HexagonConfig
import hexagon.tools.Pool


private[hexagon] class BloomFilterManager(val config: HexagonConfig) extends Closeable {


  val pool: Pool[String, BloomFilter[String]] = new Pool()


  def getBloomFilter(topic: String): BloomFilter[String] = {

    def create: BloomFilter[String] = {
      BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8),
        config.bloomFilterExpectInsertions,
        config.bloomFilterFalsePositiveProbability)
    }
    
    pool.putIfNotExists(topic, create)
    pool.get(topic)
  }


  override def close(): Unit = ???
}
