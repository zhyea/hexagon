package hexagon.tools

object Threads {

  def newThread(name: String, runnable: Runnable, isDaemon: Boolean = false): Thread = {
    val t: Thread = new Thread(runnable, name)
    t.setDaemon(isDaemon)
    t
  }

}
