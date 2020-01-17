package hexagon.utils

import java.util.Properties

import org.junit.{Assert, Before, Test}

@Test class PropKitTest {

  @Before val props: Properties = PropKit.load("/zhy/hexagon/server.properties")


  @Test def getInt(): Unit = {
    val port = PropKit.getInt(props, "port")
    Assert.assertEquals(1022, port)
  }


}
