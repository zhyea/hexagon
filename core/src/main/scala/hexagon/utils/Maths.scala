package hexagon.utils

object Maths {


  def hashcode(as: Any*): Int = {
    if (as == null)
      return 0
    var h = 1
    var i = 0
    while (i < as.length) {
      if (as(i) != null) {
        h = 31 * h + as(i).hashCode
        i += 1
      }
    }
    h
  }
}
