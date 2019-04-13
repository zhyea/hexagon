package hexagon.api

import org.junit.{Assert, Test}


class PutRequestTest {


  @Test def construct(): Unit = {
    val topic = "test"
    val msg = "message"
    val request = PutRequest(topic, msg)

    Assert.assertEquals(topic, request.topic)
    Assert.assertEquals(msg, request.msg)
    Assert.assertEquals(RequestKeys.Put, request.id)
  }

}
