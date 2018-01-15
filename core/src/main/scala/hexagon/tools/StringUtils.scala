package hexagon.tools

object StringUtils {


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

}
