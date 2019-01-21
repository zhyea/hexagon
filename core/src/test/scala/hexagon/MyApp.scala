package hexagon

import java.text.NumberFormat

import com.google.common.hash.BloomFilter

object MyApp extends App {

  val nf = NumberFormat.getInstance()
  nf.setMinimumIntegerDigits(20)
  nf.setMaximumFractionDigits(0)
  nf.setGroupingUsed(false)
  val s = nf.format(360)

  println(s)


  val bloomFilter = BloomFilter.create()

}
