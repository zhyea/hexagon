package hexagon.tools

import java.io._


object IOUtils {

	val NEW_LINE = "\r\n"


	def read(input: InputStream): String = {
		var reader: BufferedReader = null
		try {
			reader = new BufferedReader(new InputStreamReader(input))
			val builder = new StringBuilder
			var line = reader.readLine()
			while (null != line) {
				builder.append(line).append(NEW_LINE)
				line = reader.readLine()
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
