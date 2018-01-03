package hexagon.server.aio

import java.util.concurrent.TimeUnit

object BioTest extends App {

	val host = "127.0.0.1"
	val port = 8089
	val bioServer = new BioServer(port)

	new Thread(() => bioServer.start()).start()

/*	val bioClient = new BioClient(host, port)

	new Thread(() => bioClient.send("The request from client.")).start()*/

	TimeUnit.SECONDS.sleep(10)

}
