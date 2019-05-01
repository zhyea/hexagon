package hexagon.tools

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

abstract class ShutdownableThread(val name: String, val isInterruptible: Boolean = true)
  extends Thread(name) with Logging {


  this.setDaemon(false)
  val isRunning: AtomicBoolean = new AtomicBoolean(true)
  private val shutdownLatch = new CountDownLatch(1)

  def shutdown() = {
    initiateShutdown()
    awaitShutdown()
  }

  def initiateShutdown(): Boolean = {
    if(isRunning.compareAndSet(true, false)) {
      info("Shutting down")
      isRunning.set(false)
      if (isInterruptible)
        interrupt()
      true
    } else
      false
  }


  def awaitShutdown(): Unit = {
    shutdownLatch.await()
    info("Shutdown completed")
  }

  def doWork(): Unit

  override def run(): Unit = {
    info("Starting ")
    try{
      while(isRunning.get()){
        doWork()
      }
    } catch{
      case e: Throwable =>
        if(isRunning.get())
          error("Error due to ", e)
    }
    shutdownLatch.countDown()
    info("Stopped ")
  }
}
