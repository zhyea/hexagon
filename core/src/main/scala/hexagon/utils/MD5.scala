package hexagon.utils

import java.security.MessageDigest

/**
  * 用来执行MD5运算
  */
object MD5 {


	def md5(str: String): String = {
		val md = MessageDigest.getInstance("MD5")
		md.update(str.getBytes())
		val r = BigInt(1, md.digest()).toString(32)
		fill(r)
	}


	def md5ToArr(str: String): Array[Byte] = {
		val md = MessageDigest.getInstance("MD5")
		md.update(str.getBytes())
		md.digest()
	}


	@scala.annotation.tailrec
	private def fill(s: String): String = if (32 == s.length) s else fill("0" + s)


}
