package hexagon.tools

object Utils {

  def newThread(name: String, runnable: Runnable, isDaemon: Boolean): Thread = {
    val t: Thread = new Thread(runnable, name)
    t.setDaemon(false)
    t
  }

}
