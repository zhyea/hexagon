package hexagon.tools

object SysTime {

  def mills: Long = System.currentTimeMillis()

  def nanos: Long = System.nanoTime()

  def elapsed(start: Long): Long = mills - start

}
