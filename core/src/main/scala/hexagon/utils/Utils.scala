package hexagon.utils

object Utils {


	def newThread(name: String, runnable: Runnable): Thread = {
		newThread(name, runnable)
	}


	def newThread(name: String, runnable: Runnable, daemon: Boolean): Thread = {
		val thread: Thread = new Thread(runnable, name)
		thread.setDaemon(daemon)
		thread
	}

}
