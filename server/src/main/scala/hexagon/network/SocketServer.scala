package hexagon.network

import hexagon.handler.RequestHandlers
import hexagon.tools.Logging
import hexagon.utils.Threads

private[hexagon] class SocketServer(private val host: String,
                                    private val port: Int,
                                    private val handlers: RequestHandlers,
                                    private val sendBufferSize: Int,
                                    private val receiveBufferSize: Int,
                                    private val maxRequestSize: Int = Int.MaxValue) extends Logging {

  private var processor: Processor = _
  private var acceptor: Acceptor = _

  def startup(): Unit = {
    info("Starting socket server")

    processor = new Processor(handlers, maxRequestSize)
    acceptor = new Acceptor(host, port, sendBufferSize, receiveBufferSize, processor)

    Threads.newThread("Hexagon-processor", processor).start()
    processor.awaitStartup()

    Threads.newThread("Hexagon-acceptor", acceptor).start()
    acceptor.awaitStartup()

    info("Start socket server completed.")
  }


  def shutdown(): Unit = {
    info("Shutting down.")

    if (null != acceptor) acceptor.shutdown()
    if (null != processor) processor.shutdown()

    info("Shutdown completed.")
  }

}



