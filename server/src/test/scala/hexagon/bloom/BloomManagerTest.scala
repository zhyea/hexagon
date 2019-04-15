package hexagon.bloom

import hexagon.config.HexagonConfig
import hexagon.utils.PropKit
import org.junit.Test

class BloomManagerTest {

  val config = new HexagonConfig(PropKit.load("D:\\JDevelop\\workspace\\zy-dev\\hexagon\\server\\src\\test\\resources\\broker.properties"))

  val bfm = new BloomFilterManager(config)

  @Test def put(): Unit ={

    val topic = "test"
    val bf = bfm.getBloomFilter(topic)

    val r0 = bf.put("a")
    val r1 = bf.put("a")
    val r2 = bf.put("a")
    val r3 = bf.put("a")


    println(r0)
    println(r1)
    println(r2)
    println(r3)


  }

}
