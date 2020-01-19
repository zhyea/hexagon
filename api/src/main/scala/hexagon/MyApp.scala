package hexagon

import java.util.concurrent.TimeUnit

import hexagon.tools.Logging


object MyApp extends App with Logging {

	def mTime: Long = System.currentTimeMillis()

	val fTime = () => System.currentTimeMillis()

	def job(t1: => Long, t2: () => Long) = {
		for (i <- 0 to 3) {
			println(s"start time: $t1")
			println(s"  end time: ${t2()}")
			TimeUnit.SECONDS.sleep(1L)
			println("------------------------------------------------------------")
		}
	}

	job(mTime, fTime)

}
