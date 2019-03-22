package hexagon.utils

import java.util.Properties

import org.junit.{Assert, Test}

@Test class PropKitTest {

  val cfgFile: String = "D:\\JDevelop\\workspace\\cust-explore\\hexagon\\core\\src\\test\\resources\\server.properties"

  val props: Properties = PropKit.load(cfgFile)


  @Test def getString(): Unit = {
    val s = PropKit.getString(props, "string.value")
    Assert.assertEquals("127.0.0.1", s)
  }

  @Test def getInt(): Unit = {
    val i = PropKit.getInt(props, "int.value")
    Assert.assertEquals(1022, i)
  }

  @Test def getLong(): Unit = {
    val l = PropKit.getLong(props, "long.value")
    Assert.assertEquals(9999999L, l)
  }

  @Test def getDouble(): Unit = {
    val d = PropKit.getDouble(props, "double.value")
    Assert.assertEquals(1.2, d, 0)
  }


}
