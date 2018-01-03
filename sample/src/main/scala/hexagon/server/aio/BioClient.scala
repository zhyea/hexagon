package hexagon.server.aio

import java.net.Socket

import hexagon.tools.IOUtils

class BioClient(host: String, port: Int) {

	def send(message: String): Unit = {
		var socket: Socket = null
		try {
			socket = new Socket(host, port)
			val response = IOUtils.read(socket.getInputStream)
			IOUtils.write(socket.getOutputStream, message)
			println("Client Received: " + response)
		} finally {
			if (null != socket) socket.close()
		}
	}

}
