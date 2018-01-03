package hexagon.tools

import java.io.{File, FileInputStream}

import junit.framework.TestCase

class IOUtilsTest extends TestCase {

	def testRead(): Unit = {
		val file = new File("/0.log")
		val str = IOUtils.read(new FileInputStream(file))
		println(str)
	}


}
