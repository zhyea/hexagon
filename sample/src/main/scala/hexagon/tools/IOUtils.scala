package hexagon.tools

import java.io._


object IOUtils {

	val NEW_LINE = "\r\n"


	def read(input: InputStream): String = {
		var reader: BufferedReader = null
		try {
			val builder = new StringBuilder
			var buffer = new Array[Byte](1024)
			var len = input.read(buffer)
			while (len > 0) {
				builder.append(new String(buffer, 0, len))
				len = input.read(buffer)
			}
			builder.toString
		} finally {
			if (null != input) input.close()
			if (null != reader) reader.close()
		}
	}


	def write(out: OutputStream, response: String): Unit = {
		var writer: PrintWriter = null
		try {
			writer = new PrintWriter(out, true)
			writer.println(response)
		} finally {
			if (null != out) out.close()
			if (null != writer) writer.close()
		}
	}


}
