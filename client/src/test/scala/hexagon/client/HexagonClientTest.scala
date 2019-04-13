package hexagon.client

import org.junit.Test


class HexagonClientTest {

  val client = new HexagonClient("127.0.0.1", 8190)

  @Test def put(): Unit = {

    val topic = "test"
    val list = List("this", "is", "a", "test", "just", "a", "test")
    list.foreach(e => {
      val r = client.put(topic, e)
      println(r)
    })
  }


}
