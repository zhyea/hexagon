package hexagon.server

import java.nio.channels.Selector
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

import hexagon.tools.Logging


private abstract class AbstractServerThread() extends Runnable with Logging {

	protected val selector: Selector = Selector.open()
	private val startupLatch = new CountDownLatch(1)
	private val shutdownLatch = new CountDownLatch(1)
	private val alive = new AtomicBoolean(false)


	def awaitStartup(): Unit = startupLatch.await()


	def startupComplete(): Unit = {
		alive.set(true)
		startupLatch.countDown()
	}


	def shutdown(): Unit = {
		selector.wakeup()
		shutdownLatch.await()
		alive.set(false)
	}

	def shutdownComplete(): Unit = shutdownLatch.countDown()


	def isRunning: Boolean = alive.get()
}

