package hexagon.utils


object SysTime {

	def milli: Long = System.currentTimeMillis()

	def nano: Long = System.nanoTime()

	def elapsed(start: Long): Long = milli - start

	def sleep(sleepTime: Long): Unit = Thread.sleep(sleepTime)

}
