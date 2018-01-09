package hexagon.server.bio

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.Socket
import java.util.concurrent.TimeUnit


class BioClient(host: String, port: Int) {

	def send(message: String): Unit = {
		var socket: Socket = null
		try {
			socket = new Socket(host, port)
			println("Client Started.")

			val input = socket.getInputStream
			val reader = new BufferedReader(new InputStreamReader(input))
			val writer = new PrintWriter(socket.getOutputStream, true)


			writer.println(message)
			writer.println()

			TimeUnit.SECONDS.sleep(30)

			var line = reader.readLine()
			while (null != line) {
				println("Client Received: " + line)
				line = reader.readLine()
			}


			input.close()
			reader.close()
			writer.close()

		} finally {
			if (null != socket) socket.close()
		}
	}

}
