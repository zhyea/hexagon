package hexagon.server.nio

import java.util.concurrent.TimeUnit

object NioTest extends App {

  val server = new NioServer(8089)

  val client = new NioClient("127.0.0.1", 8089)

  new Thread(() => server.start()).start()
  new Thread(() => client.start()).start()


  TimeUnit.SECONDS.sleep(10)

  client.send("A message from client.")

  TimeUnit.SECONDS.sleep(120)

}
