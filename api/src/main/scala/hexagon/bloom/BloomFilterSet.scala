package hexagon.bloom

import java.util.concurrent.{ConcurrentNavigableMap, ConcurrentSkipListMap}

import com.google.common.hash.BloomFilter

class BloomFilterSet(topic: String) {

  private val bloomFilters: ConcurrentNavigableMap[Long, BloomFilter[String]]
  = new ConcurrentSkipListMap[Long, BloomFilter[String]]


}
