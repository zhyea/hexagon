package hexagon.tools

import junit.framework.TestCase

class StringUtilsTest extends TestCase {


  def testIsBlank(): Unit = {
    val source1 = ""
    val source2 = "  "
    val source3 = "  "
    val source4 = " 111 "

    println(StringUtils.isBlank(source1))
    println(StringUtils.isBlank(source2))
    println(StringUtils.isBlank(source3))
    println(StringUtils.isBlank(source4))
  }


}
