package hexagon.server.bio

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{ServerSocket, Socket}


class BioServer(val port: Int) {

	def start(): Unit = {
		var serverSocket: ServerSocket = null
		try {
			serverSocket = new ServerSocket(port)
			println("Server Started.")
			while (true) {
				val socket = serverSocket.accept()
				new Thread(() => handle(socket)).start()
			}
		} finally {
			if (null != serverSocket) serverSocket.close()
		}
	}

	def handle(socket: Socket): Unit = {
		println("Start new thread.....")
		try {
			val input = socket.getInputStream
			val reader = new BufferedReader(new InputStreamReader(input))
			val writer = new PrintWriter(socket.getOutputStream, true)

			var line = reader.readLine()
			while (null != line) {
				println("Server Received: " + line)
				line = reader.readLine()
				writer.println("a")
			}


			input.close()
			reader.close()
			writer.close()
		} finally {
			socket.close()
		}
	}
}
