package hexagon.server.nio

import java.util.concurrent.TimeUnit

object NioTest extends App {

  val server = new NioServer(8089)

  new Thread(() => server.start()).start()

  TimeUnit.SECONDS.sleep(120)

}
