package hexagon.bloom

import com.google.common.hash.BloomFilter


object BloomFilterManager {


  val map: Map[String, Seq[BloomFilter[String]]] = Map()



}
