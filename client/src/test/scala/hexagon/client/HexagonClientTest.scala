package hexagon.client

import java.util.concurrent.TimeUnit

import org.junit.Test


class HexagonClientTest {

  val client = new HexagonClient("127.0.0.1", 8190, socketTimeout = TimeUnit.MINUTES.toMillis(1L).toInt)

  @Test def put(): Unit = {

    val topic = "test"
    val list = List("this", "is", "a", "test", "just", "a", "test")
    try {
      list.foreach(e => {
        val r = client.put(topic, e)
        println(r)
      })
    } finally {
      client.close()
    }

  }


}
