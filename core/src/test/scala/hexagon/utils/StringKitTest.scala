package hexagon.utils

import org.junit.{Assert, Test}

@Test class StringKitTest {


  @Test def isBlankTest(): Unit = {

    import StringKit.isBlank

    Assert.assertTrue(isBlank(""))
    Assert.assertTrue(isBlank("  "))
    Assert.assertTrue(isBlank("  "))
    Assert.assertFalse(isBlank(" 111 "))
  }


}
