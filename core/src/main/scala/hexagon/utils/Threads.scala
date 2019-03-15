package hexagon.utils


/**
  * 线程操作工具类
  */
object Threads {


  /**
    * 将func包装为Runnable接口实现
    */
  def runnable(func: => Unit): Runnable = () => func


  /**
    * 创建一个守护线程
    */
  def daemonThread(name: String, func: => Unit): Thread =
    newThread(name, runnable(func), isDaemon = true)


  /**
    * 创建一个守护线程
    */
  def daemonThread(name: String, runnable: Runnable): Thread =
    newThread(name, runnable, isDaemon = true)


  /**
    * 创建一个新线程
    */
  def newThread(name: String, runnable: Runnable, isDaemon: Boolean = false): Thread = {
    val t: Thread = new Thread(runnable, name)
    t.setDaemon(isDaemon)
    t
  }


}
