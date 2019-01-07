package hexagon

import java.text.NumberFormat

object MyApp extends App {

  val nf = NumberFormat.getInstance()
  nf.setMinimumIntegerDigits(20)
  nf.setMaximumFractionDigits(0)
  nf.setGroupingUsed(false)
  val s = nf.format(360)

  println(s)

}
