package hexagon.tools

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ScheduledThreadPoolExecutor, ThreadFactory, TimeUnit}

import hexagon.utils.Threads


/**
  * 定时任务管理工具
  */
class HexagonScheduler(val numThreads: Int,
                       val baseThreadName: String,
                       isDaemon: Boolean) extends Logging {

  private val threadId = new AtomicLong(0)

  private val executor = new ScheduledThreadPoolExecutor(numThreads, new ThreadFactory {
    override def newThread(runnable: Runnable): Thread =
      Threads.newThread(baseThreadName + threadId.getAndIncrement(), runnable, isDaemon)
  })

  executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false)
  executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false)


  def shutdownNow(): Unit = {
    executor.shutdownNow()
    info(s"Shutting down scheduler $baseThreadName")
  }

  def shutdown(): Unit = {
    executor.shutdown()
    executor.awaitTermination(1, TimeUnit.DAYS)
    info(s"Shutting down scheduler $baseThreadName")
  }


  def schedule(name: String, func: () => Unit, delay: Long, period: Long, unit: TimeUnit): Unit = {
    info(s"Scheduling task $name with initial delay ${unit.toMillis(delay)} ms and period ${unit.toMillis(period)} ms.")

    val runnable = Threads.runnable {
      try {
        trace(s"Beginning execution of scheduled task '$name'.")
        func()
      } catch {
        case t: Throwable => error(s"Uncaught Exception in scheduled task '$name'.", t)
      } finally {
        trace(s"Completed execution of scheduled task '$name'.")
      }
    }

    if (period > 0)
      executor.scheduleAtFixedRate(runnable, delay, period, unit)
    else
      executor.schedule(runnable, delay, unit)
  }
}