package hexagon.utils

import org.junit.{Assert, Test}

@Test class StrUtilsTest {


  @Test def isBlank(): Unit = {

    import StrUtils.isBlank

    Assert.assertTrue(isBlank(""))
    Assert.assertTrue(isBlank("  "))
    Assert.assertTrue(isBlank("  "))
    Assert.assertFalse(isBlank(" 111 "))
  }


}
