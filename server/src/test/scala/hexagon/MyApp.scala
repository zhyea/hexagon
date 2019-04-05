package hexagon

import java.io.FileWriter

import scala.collection.mutable.ListBuffer

object MyApp extends App {

  println(0xEF.toByte)
  println(Array(0xEF, 0xBB, 0xBF).map(_.toByte).toSeq)

  val bom = Array(0xEF, 0xBB, 0xBF).map(_.toByte)
  val result = ListBuffer(new String(bom, "UTF8") + "title\n")
  result += "en-chars\n"
  result += "中文字符"
  val w = new FileWriter("/my-file.csv")
  result.foreach(w.write)
  w.flush()

}
