package hexagon.tools

object SysTime {

  def mills: Long = System.currentTimeMillis()

  def diff(start: Long): Long = mills - start

}
