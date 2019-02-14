package hexagon.utils

import org.junit.{Assert, Test}

@Test class StringUtilsTest {


  @Test def isBlankTest(): Unit = {

    import StringUtils.isBlank

    Assert.assertTrue(isBlank(""))
    Assert.assertTrue(isBlank("  "))
    Assert.assertTrue(isBlank("  "))
    Assert.assertFalse(isBlank(" 111 "))
  }


}
