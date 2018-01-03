package hexagon.server.aio

import java.net.ServerSocket

import hexagon.tools.IOUtils

class BioServer(val port: Int) {

	def start(): Unit = {
		var serverSocket: ServerSocket = null
		try {
			serverSocket = new ServerSocket(port)
			while (true) {
				val socket = serverSocket.accept()
				val input = socket.getInputStream
				println("Server Received: " + IOUtils.read(input))
				val out = socket.getOutputStream
				IOUtils.write(out, "We've received.")
			}
		} finally {
			if (null != serverSocket) serverSocket.close()
		}
	}
}
