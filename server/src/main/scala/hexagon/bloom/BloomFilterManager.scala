package hexagon.bloom

import java.nio.charset.StandardCharsets

import com.google.common.hash.{BloomFilter, Funnels}
import hexagon.config.HexagonConfig


private[hexagon] class BloomFilterManager(val config: HexagonConfig) {


  val map: Map[String, Seq[BloomFilter[String]]] = Map()


  def getBloomFilter(): BloomFilter[String] = {
    BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8),
      config.bloomfilterExpectInsertions,
      config.bloomfilterFalsePositiveProbability)
  }

}
