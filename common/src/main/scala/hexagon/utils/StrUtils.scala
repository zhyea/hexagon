package hexagon.utils

import java.nio.charset.StandardCharsets

import hexagon.exceptions.HexagonException

object StrUtils {


  def isBlank(source: String): Boolean = {
    if (null == source) return true
    val len = source.length
    if (0 == len) return true
    for (i <- 0 until len)
      if (!Character.isWhitespace(source.charAt(i)))
        return false
    true
  }


  def isNotBlank(source: String): Boolean = !isBlank(source)


  def shortStringLength(string: String): Int = {
    if (null == string) {
      java.lang.Short.BYTES
    } else {
      val encodedString = string.getBytes(StandardCharsets.UTF_8)
      if (encodedString.length > Short.MaxValue) {
        throw new HexagonException(s"String exceeds the maximum size of ${Short.MaxValue}.")
      } else {
        java.lang.Short.BYTES + encodedString.length
      }
    }
  }

}
