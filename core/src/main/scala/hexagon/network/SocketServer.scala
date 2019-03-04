package hexagon.network

import hexagon.tools.Logging
import hexagon.utils.Threads

private[hexagon] class SocketServer(private val host: String,
                                    private val port: Int,
                                    private val numProcessorThreads: Int,
                                    private val sendBufferSize: Int,
                                    private val receiveBufferSize: Int,
                                    private val maxRequestSize: Int = Int.MaxValue) extends Logging {

  private val processors = new Array[Processor](numProcessorThreads)
  private var acceptor: Acceptor = _

  def startup(): Unit = {
    info("Starting socket server")

    for (i <- 0 until numProcessorThreads) {
      processors(i) = new Processor(i, maxRequestSize)
      Threads.newThread(s"Hexagon-processor-$i", processors(i)).start()
    }

    acceptor = new Acceptor(host, port, sendBufferSize, receiveBufferSize, processors)
    Threads.newThread("Hexagon-acceptor", acceptor, false).start()
    acceptor.awaitStartup()

    info("Start socket server completed.")
  }


  def shutdown(): Unit = {
    info("Shutting down.")

    if (null != acceptor) acceptor.shutdown()
    processors.foreach(_.shutdown())

    info("Shutdown completed.")
  }

}



